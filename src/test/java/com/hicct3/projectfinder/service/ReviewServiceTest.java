package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.project.review.CreateReviewRequestDTO;
import com.hicct3.projectfinder.dto.project.review.MyReviewsResponseDTO;
import com.hicct3.projectfinder.entity.MemberReview;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectMember;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.MemberReviewRepository;
import com.hicct3.projectfinder.repository.ProjectMemberRepository;
import com.hicct3.projectfinder.repository.ProjectRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private MemberReviewRepository memberReviewRepository;
    @Mock private UserRepository userRepository;
    @Mock private Clock clock;

    @InjectMocks
    private ReviewService reviewService;

    private User author;
    private User target;
    private Project completedProject;

    @BeforeEach
    void setUp() {
        author = User.builder().userId(1L).nickName("작성자").build();
        target = User.builder()
                .userId(2L)
                .nickName("대상")
                .ratingAvg(0.0)
                .ratingCount(0)
                .build();
        completedProject = Project.builder()
                .id(10L)
                .title("완료 프로젝트")
                .status(ProjectStatus.COMPLETED)
                .build();
    }

    @Test
    void savesOneTargetReviewAndUpdatesTargetRating() {
        stubReviewCreationAccess();
        stubClock();
        MemberReview saved = review(5, 4, 3, "좋은 팀원이었습니다.");
        when(memberReviewRepository.saveAndFlush(any())).thenReturn(saved);
        when(memberReviewRepository.findAllByTargetAndHiddenAtIsNull(target))
                .thenReturn(List.of(saved));

        reviewService.createReview(1L, 10L, request(5, 4, 3));

        ArgumentCaptor<MemberReview> captor = ArgumentCaptor.forClass(MemberReview.class);
        verify(memberReviewRepository).saveAndFlush(captor.capture());
        assertEquals(target, captor.getValue().getTarget());
        assertEquals(4.0, target.getRatingAvg());
        assertEquals(1, target.getRatingCount());
    }

    @Test
    void rejectsDuplicateTargetReview() {
        stubReviewCreationAccess();
        when(memberReviewRepository.existsByAuthorAndProjectAndTarget(
                author, completedProject, target)).thenReturn(true);

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> reviewService.createReview(1L, 10L, request(5, 5, 5)));

        assertEquals(ErrorCode.ALREADY_REVIEWED, exception.getErrorCode());
        verify(memberReviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsReviewBeforeProjectCompletion() {
        completedProject.setStatus(ProjectStatus.IN_PROGRESS);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(completedProject));

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> reviewService.createReview(1L, 10L, request(5, 5, 5)));

        assertEquals(ErrorCode.PROJECT_NOT_COMPLETED, exception.getErrorCode());
    }

    @Test
    void requestScoresMustBeBetweenOneAndFive() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        CreateReviewRequestDTO request = request(0, 6, 5);

        assertEquals(2, validator.validate(request).size());
    }

    @Test
    void allowsReviewWithoutComment() {
        stubReviewCreationAccess();
        stubClock();
        CreateReviewRequestDTO request = request(5, 4, 3);
        request.setComment(null);
        when(memberReviewRepository.findAllByTargetAndHiddenAtIsNull(target)).thenReturn(List.of());

        reviewService.createReview(1L, 10L, request);

        ArgumentCaptor<MemberReview> captor = ArgumentCaptor.forClass(MemberReview.class);
        verify(memberReviewRepository).saveAndFlush(captor.capture());
        assertNull(captor.getValue().getComment());
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void countsOnlyProjectsWithPendingTeamReviews() {
        ProjectMember membership = ProjectMember.builder()
                .user(author)
                .project(completedProject)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(projectMemberRepository.findAllByUserAndProject_StatusInOrderByProject_UpdatedAtDesc(
                author, List.of(ProjectStatus.COMPLETED)))
                .thenReturn(List.of(membership));
        when(memberReviewRepository.countByAuthorAndProject(author, completedProject)).thenReturn(1L);
        when(projectMemberRepository.countByProjectAndUserNot(completedProject, author)).thenReturn(2L);

        assertEquals(1L, reviewService.getPendingReviewProjectCount(1L));
    }

    @Test
    void receivedSummaryUsesOneAveragePerReview() {
        MemberReview first = review(5, 4, 5, "첫 번째");
        MemberReview second = review(3, 4, 4, "두 번째");
        PageRequest pageable = PageRequest.of(0, 10);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(memberReviewRepository.findAllByTargetAndHiddenAtIsNull(target))
                .thenReturn(List.of(first, second));
        when(memberReviewRepository.findAllByTargetAndHiddenAtIsNull(target, pageable))
                .thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));

        MyReviewsResponseDTO response = reviewService.getReceivedReviews(2L, pageable);

        assertEquals(4.2, response.getRatingAvg());
        assertEquals(2, response.getRatingCount());
        assertEquals(1, response.getScore4());
        assertEquals(1, response.getScore5());
        assertEquals(2, response.getReviews().getTotalElements());
    }

    @Test
    void hiddenReviewIsExcludedFromTargetRating() {
        stubClock();
        MemberReview hidden = review(1, 1, 1, "숨길 후기");
        MemberReview visible = review(5, 5, 5, "표시할 후기");
        when(memberReviewRepository.findById(1L)).thenReturn(Optional.of(hidden));
        when(memberReviewRepository.findAllByTargetAndHiddenAtIsNull(target))
                .thenReturn(List.of(visible));

        reviewService.updateHidden(1L, true);

        assertTrue(hidden.isHidden());
        assertEquals(5.0, target.getRatingAvg());
        assertEquals(1, target.getRatingCount());

        reviewService.updateHidden(1L, false);
        assertFalse(hidden.isHidden());
    }

    private void stubReviewCreationAccess() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(completedProject));
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(projectMemberRepository.existsByUserAndProject(author, completedProject))
                .thenReturn(true);
        when(projectMemberRepository.existsByUserAndProject(target, completedProject))
                .thenReturn(true);
    }

    private void stubClock() {
        when(clock.instant()).thenReturn(Instant.parse("2026-08-10T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
    }

    private CreateReviewRequestDTO request(int contribution, int participation, int responsibility) {
        return CreateReviewRequestDTO.builder()
                .targetUserId(2L)
                .contribution(contribution)
                .participation(participation)
                .responsibility(responsibility)
                .comment("좋은 팀원이었습니다.")
                .build();
    }

    private MemberReview review(int contribution, int participation, int responsibility, String comment) {
        return MemberReview.builder()
                .id(1L)
                .project(completedProject)
                .author(author)
                .target(target)
                .contribution(contribution)
                .participation(participation)
                .responsibility(responsibility)
                .comment(comment)
                .createdAt(LocalDateTime.of(2026, 8, 10, 9, 0))
                .build();
    }
}
