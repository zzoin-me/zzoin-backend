package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.project.MyReviewableProjectResponseDTO;
import com.hicct3.projectfinder.dto.common.PageResponseDTO;
import com.hicct3.projectfinder.dto.project.review.*;
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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MemberReviewRepository memberReviewRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public void createReview(Long authorId, Long projectId, CreateReviewRequestDTO request) {
        Project project = findCompletedProject(projectId);
        User author = findUser(authorId);
        User target = findUser(request.getTargetUserId());

        requireProjectMember(author, project);
        if (author.getUserId().equals(target.getUserId())) {
            throw new GeneralException(ErrorCode.CANNOT_REVIEW_SELF);
        }
        if (!projectMemberRepository.existsByUserAndProject(target, project)) {
            throw new GeneralException(ErrorCode.REVIEW_TARGET_INVALID);
        }
        if (memberReviewRepository.existsByAuthorAndProjectAndTarget(author, project, target)) {
            throw new GeneralException(ErrorCode.ALREADY_REVIEWED);
        }
        String comment = request.getComment();
        String normalizedComment = comment == null || comment.isBlank() ? null : comment.trim();

        MemberReview review = MemberReview.builder()
                .contribution(request.getContribution())
                .participation(request.getParticipation())
                .responsibility(request.getResponsibility())
                .comment(normalizedComment)
                .author(author)
                .target(target)
                .project(project)
                .createdAt(LocalDateTime.now(clock))
                .build();

        try {
            memberReviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException exception) {
            throw new GeneralException(ErrorCode.ALREADY_REVIEWED);
        }
        recalculateUserRating(target);
    }

    @Transactional(readOnly = true)
    public MembersResponseDTO getMembers(Long userId, Long projectId) {
        Project project = findProject(projectId);
        User user = findUser(userId);
        requireProjectMember(user, project);
        return MembersResponseDTO.builder()
                .members(projectMemberRepository.findAllByProject(project).stream()
                        .map(MemberResponseDTO::from)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public ReviewTargetsResponseDTO getReviewTargets(Long userId, Long projectId) {
        Project project = findCompletedProject(projectId);
        User user = findUser(userId);
        requireProjectMember(user, project);

        List<ReviewTargetResponseDTO> targets = projectMemberRepository.findAllByProject(project)
                .stream()
                .filter(member -> !member.getUser().getUserId().equals(userId))
                .map(member -> ReviewTargetResponseDTO.builder()
                        .userId(member.getUser().getUserId())
                        .nickname(member.getUser().getNickName())
                        .recruitment(member.getJobName())
                        .profileUrl(member.getUser().getProfileUrl())
                        .reviewed(memberReviewRepository.existsByAuthorAndProjectAndTarget(
                                user, project, member.getUser()))
                        .build())
                .toList();
        long reviewedCount = targets.stream().filter(ReviewTargetResponseDTO::getReviewed).count();

        return ReviewTargetsResponseDTO.builder()
                .projectId(project.getId())
                .projectTitle(project.getTitle())
                .totalTargetCount((long) targets.size())
                .reviewedTargetCount(reviewedCount)
                .targets(targets)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<MyReviewableProjectResponseDTO> getMyReviewableProjects(
            Long userId,
            Pageable pageable
    ) {
        User user = findUser(userId);
        return projectMemberRepository.findByUserAndProject_Status(
                        user, ProjectStatus.COMPLETED, pageable)
                .map(member -> {
                    Project project = member.getProject();
                    long reviewedCount = memberReviewRepository.countByAuthorAndProject(user, project);
                    long targetCount = projectMemberRepository.countByProjectAndUserNot(project, user);
                    return MyReviewableProjectResponseDTO.builder()
                            .projectId(project.getId())
                            .title(project.getTitle())
                            .recruitment(member.getJobName())
                            .joinedAt(member.getJoinedAt())
                            .completedAt(member.getCompletedAt())
                            .totalTargetCount(targetCount)
                            .reviewedTargetCount(reviewedCount)
                            .reviewCompleted(reviewedCount >= targetCount)
                            .build();
                });
    }

    @Transactional(readOnly = true)
    public long getPendingReviewProjectCount(Long userId) {
        User user = findUser(userId);
        return projectMemberRepository
                .findAllByUserAndProject_StatusInOrderByProject_UpdatedAtDesc(
                        user, List.of(ProjectStatus.COMPLETED))
                .stream()
                .filter(member -> {
                    Project project = member.getProject();
                    long reviewedCount = memberReviewRepository.countByAuthorAndProject(user, project);
                    long targetCount = projectMemberRepository.countByProjectAndUserNot(project, user);
                    return reviewedCount < targetCount;
                })
                .count();
    }

    @Transactional(readOnly = true)
    public MemberReviewsResponseDTO getMyProjectReviews(Long userId, Long projectId) {
        Project project = findProject(projectId);
        User user = findUser(userId);
        requireProjectMember(user, project);

        Map<User, List<String>> recruitmentMap = projectMemberRepository.findAllByProject(project)
                .stream()
                .collect(Collectors.groupingBy(
                        ProjectMember::getUser,
                        Collectors.mapping(ProjectMember::getJobName, Collectors.toList())));

        List<MemberReviewResponseDTO> reviews = memberReviewRepository
                .findAllByAuthorAndProject(user, project)
                .stream()
                .map(review -> MemberReviewResponseDTO.builder()
                        .reviewId(review.getId())
                        .targetUserId(review.getTarget().getUserId())
                        .nickname(review.getTarget().getNickName())
                        .recruitments(recruitmentMap.get(review.getTarget()))
                        .profileUrl(review.getTarget().getProfileUrl())
                        .contribution(review.getContribution())
                        .participation(review.getParticipation())
                        .responsibility(review.getResponsibility())
                        .avgRating(roundOneDecimal(review.getAverage()))
                        .comment(review.isHidden() ? null : review.getComment())
                        .createdAt(review.getCreatedAt())
                        .hidden(review.isHidden())
                        .build())
                .toList();
        return MemberReviewsResponseDTO.builder().members(reviews).build();
    }

    @Transactional(readOnly = true)
    public MyReviewsResponseDTO getReceivedReviews(Long userId, Pageable pageable) {
        User user = findUser(userId);
        List<MemberReview> visibleReviews = memberReviewRepository
                .findAllByTargetAndHiddenAtIsNull(user);
        Page<ReceivedReviewResponseDTO> reviews = memberReviewRepository
                .findAllByTargetAndHiddenAtIsNull(user, pageable)
                .map(review -> ReceivedReviewResponseDTO.builder()
                        .reviewId(review.getId())
                        .projectId(review.getProject().getId())
                        .projectTitle(review.getProject().getTitle())
                        .contribution(review.getContribution())
                        .participation(review.getParticipation())
                        .responsibility(review.getResponsibility())
                        .avgRating(roundOneDecimal(review.getAverage()))
                        .comment(review.getComment())
                        .createdAt(review.getCreatedAt())
                        .build());

        return buildReceivedReviewsResponse(visibleReviews, reviews);
    }

    @Transactional(readOnly = true)
    public Page<WrittenReviewResponseDTO> getWrittenReviews(Long userId, Pageable pageable) {
        User user = findUser(userId);
        return memberReviewRepository.findAllByAuthor(user, pageable)
                .map(review -> WrittenReviewResponseDTO.builder()
                        .reviewId(review.getId())
                        .projectId(review.getProject().getId())
                        .projectTitle(review.getProject().getTitle())
                        .targetUserId(review.getTarget().getUserId())
                        .targetNickname(review.getTarget().getNickName())
                        .targetProfileUrl(review.getTarget().getProfileUrl())
                        .contribution(review.getContribution())
                        .participation(review.getParticipation())
                        .responsibility(review.getResponsibility())
                        .avgRating(roundOneDecimal(review.getAverage()))
                        .comment(review.isHidden() ? null : review.getComment())
                        .createdAt(review.getCreatedAt())
                        .hidden(review.isHidden())
                        .build());
    }

    @Transactional
    public void updateHidden(Long reviewId, boolean hidden) {
        MemberReview review = memberReviewRepository.findById(reviewId)
                .orElseThrow(() -> new GeneralException(ErrorCode.REVIEW_NOT_FOUND));
        review.setHidden(hidden, LocalDateTime.now(clock));
        recalculateUserRating(review.getTarget());
    }

    private MyReviewsResponseDTO buildReceivedReviewsResponse(
            List<MemberReview> reviews,
            Page<ReceivedReviewResponseDTO> page
    ) {
        int[] distribution = new int[6];
        double contributionTotal = 0;
        double participationTotal = 0;
        double responsibilityTotal = 0;
        for (MemberReview review : reviews) {
            distribution[Math.max(1, Math.min(5, (int) Math.round(review.getAverage())))]++;
            contributionTotal += review.getContribution();
            participationTotal += review.getParticipation();
            responsibilityTotal += review.getResponsibility();
        }
        int count = reviews.size();
        double totalAverage = reviews.stream()
                .mapToDouble(MemberReview::getAverage)
                .average()
                .orElse(0.0);
        return MyReviewsResponseDTO.builder()
                .ratingAvg(roundOneDecimal(totalAverage))
                .ratingCount(count)
                .contributionAvg(roundOneDecimal(count == 0 ? 0 : contributionTotal / count))
                .participationAvg(roundOneDecimal(count == 0 ? 0 : participationTotal / count))
                .responsibilityAvg(roundOneDecimal(count == 0 ? 0 : responsibilityTotal / count))
                .score1(distribution[1])
                .score2(distribution[2])
                .score3(distribution[3])
                .score4(distribution[4])
                .score5(distribution[5])
                .reviews(PageResponseDTO.from(page))
                .build();
    }

    private void recalculateUserRating(User target) {
        List<MemberReview> reviews = memberReviewRepository.findAllByTargetAndHiddenAtIsNull(target);
        double average = reviews.stream()
                .mapToDouble(MemberReview::getAverage)
                .average()
                .orElse(0.0);
        target.setRatingAvg(roundOneDecimal(average));
        target.setRatingCount(reviews.size());
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private Project findCompletedProject(Long projectId) {
        Project project = findProject(projectId);
        if (project.getStatus() != ProjectStatus.COMPLETED) {
            throw new GeneralException(ErrorCode.PROJECT_NOT_COMPLETED);
        }
        return project;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
    }

    private void requireProjectMember(User user, Project project) {
        if (!projectMemberRepository.existsByUserAndProject(user, project)) {
            throw new GeneralException(ErrorCode.USER_NOT_IN_PROJECT);
        }
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
