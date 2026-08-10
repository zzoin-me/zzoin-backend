package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.application.AnswerRequestDTO;
import com.hicct3.projectfinder.dto.application.ApplyProjectRequestDTO;
import com.hicct3.projectfinder.dto.application.DeleteProjectRequestDTO;
import com.hicct3.projectfinder.entity.ApplicationAnswer;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectApplication;
import com.hicct3.projectfinder.entity.ProjectQuestion;
import com.hicct3.projectfinder.entity.ProjectRecruitment;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.ApplicationStatus;
import com.hicct3.projectfinder.entity.enums.NotificationType;
import com.hicct3.projectfinder.entity.enums.QuestionType;
import com.hicct3.projectfinder.event.ApplicationNotificationEvent;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.ApplicationAnswerRepository;
import com.hicct3.projectfinder.repository.ProjectApplicationRepository;
import com.hicct3.projectfinder.repository.ProjectMemberRepository;
import com.hicct3.projectfinder.repository.ProjectQuestionRepository;
import com.hicct3.projectfinder.repository.ProjectRecruitmentRepository;
import com.hicct3.projectfinder.repository.ProjectRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectApplicationServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectRecruitmentRepository projectRecruitmentRepository;
    @Mock
    private ProjectApplicationRepository projectApplicationRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectQuestionRepository projectQuestionRepository;
    @Mock
    private ApplicationAnswerRepository applicationAnswerRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private Clock clock;

    @InjectMocks
    private ProjectApplicationService projectApplicationService;

    private Project project;
    private ProjectRecruitment recruitment;
    private User applicant;

    @BeforeEach
    void setUp() {
        project = org.mockito.Mockito.mock(Project.class);
        recruitment = org.mockito.Mockito.mock(ProjectRecruitment.class);
        applicant = org.mockito.Mockito.mock(User.class);
        org.mockito.Mockito.lenient()
                .when(clock.instant()).thenReturn(Instant.parse("2026-08-10T00:00:00Z"));
        org.mockito.Mockito.lenient()
                .when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
    }

    @Test
    void savesValidatedAnswersWhenApplying() {
        stubApplyContext();
        when(project.getId()).thenReturn(30L);
        when(project.getTitle()).thenReturn("테스트 프로젝트");
        when(applicant.getNickName()).thenReturn("지원자");
        ProjectQuestion textQuestion = question(1L, QuestionType.TEXT, true, null, 0);
        ProjectQuestion singleQuestion = question(
                2L,
                QuestionType.SINGLE_CHOICE,
                false,
                "온라인,오프라인,혼합",
                1
        );
        ProjectQuestion multiQuestion = question(
                3L,
                QuestionType.MULTI_CHOICE,
                true,
                "GitHub,Notion,Slack",
                2
        );
        when(projectQuestionRepository.findAllByProjectAndDeletedAtIsNullOrderByIdAsc(project))
                .thenReturn(List.of(textQuestion, singleQuestion, multiQuestion));

        ApplyProjectRequestDTO request = ApplyProjectRequestDTO.builder()
                .recruitmentId(10L)
                .letter("프로젝트에 성실하게 참여하겠습니다.")
                .answers(List.of(
                        answer(1L, " 관련 경험이 있습니다. "),
                        answer(2L, "온라인"),
                        answer(3L, "Slack, GitHub")
                ))
                .build();

        projectApplicationService.applyProject(2L, request);

        ArgumentCaptor<ProjectApplication> applicationCaptor =
                ArgumentCaptor.forClass(ProjectApplication.class);
        verify(projectApplicationRepository).save(applicationCaptor.capture());
        assertEquals(project, applicationCaptor.getValue().getProject());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<ApplicationAnswer>> answersCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(applicationAnswerRepository).saveAll(answersCaptor.capture());

        List<ApplicationAnswer> savedAnswers = new ArrayList<>();
        answersCaptor.getValue().forEach(savedAnswers::add);

        assertEquals(3, savedAnswers.size());
        assertEquals("관련 경험이 있습니다.", savedAnswers.get(0).getAnswerText());
        assertEquals("온라인", savedAnswers.get(1).getAnswerText());
        assertEquals("GitHub,Slack", savedAnswers.get(2).getAnswerText());
        verify(recruitment).setApplicantCount(1);

        ArgumentCaptor<ApplicationNotificationEvent> eventCaptor =
                ArgumentCaptor.forClass(ApplicationNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(1L, eventCaptor.getValue().recipientId());
        assertEquals(NotificationType.APPLICATION_RECEIVED, eventCaptor.getValue().type());
        assertEquals("/projects/30/manage#applicants", eventCaptor.getValue().targetUrl());
    }

    @Test
    void publishesApprovalNotificationForApplicant() {
        ProjectApplication application = stubStatusUpdateContext();

        projectApplicationService.updateApplicantStatus(
                1L,
                20L,
                com.hicct3.projectfinder.dto.application.UpdateApplicantStatusDTO.builder()
                        .status(ApplicationStatus.APPROVED)
                        .build()
        );

        verify(application).setStatus(ApplicationStatus.APPROVED);
        verify(projectMemberRepository).save(any());

        ArgumentCaptor<ApplicationNotificationEvent> eventCaptor =
                ArgumentCaptor.forClass(ApplicationNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(2L, eventCaptor.getValue().recipientId());
        assertEquals(NotificationType.APPLICATION_APPROVED, eventCaptor.getValue().type());
        assertEquals("/mypage/applications", eventCaptor.getValue().targetUrl());
    }

    @Test
    void publishesRejectionNotificationForApplicant() {
        ProjectApplication application = stubStatusUpdateContext();

        projectApplicationService.updateApplicantStatus(
                1L,
                20L,
                com.hicct3.projectfinder.dto.application.UpdateApplicantStatusDTO.builder()
                        .status(ApplicationStatus.REJECTED)
                        .build()
        );

        verify(application).setStatus(ApplicationStatus.REJECTED);
        verify(projectMemberRepository, never()).save(any());

        ArgumentCaptor<ApplicationNotificationEvent> eventCaptor =
                ArgumentCaptor.forClass(ApplicationNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(2L, eventCaptor.getValue().recipientId());
        assertEquals(NotificationType.APPLICATION_REJECTED, eventCaptor.getValue().type());
    }

    @Test
    void rejectsApplicationWhenRequiredAnswerIsMissing() {
        stubApplyContext();
        ProjectQuestion requiredQuestion = question(1L, QuestionType.TEXT, true, null, 0);
        when(projectQuestionRepository.findAllByProjectAndDeletedAtIsNullOrderByIdAsc(project))
                .thenReturn(List.of(requiredQuestion));

        ApplyProjectRequestDTO request = ApplyProjectRequestDTO.builder()
                .recruitmentId(10L)
                .letter("프로젝트에 성실하게 참여하겠습니다.")
                .answers(List.of())
                .build();

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> projectApplicationService.applyProject(2L, request)
        );

        assertEquals(ErrorCode.QUESTION_ANSWER_REQUIRED, exception.getErrorCode());
        verify(projectApplicationRepository, never()).save(any());
    }

    @Test
    void rejectsAnswerForQuestionFromAnotherProject() {
        stubApplyContext();
        when(projectQuestionRepository.findAllByProjectAndDeletedAtIsNullOrderByIdAsc(project))
                .thenReturn(List.of());

        ApplyProjectRequestDTO request = ApplyProjectRequestDTO.builder()
                .recruitmentId(10L)
                .letter("프로젝트에 성실하게 참여하겠습니다.")
                .answers(List.of(answer(999L, "다른 프로젝트 답변")))
                .build();

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> projectApplicationService.applyProject(2L, request)
        );

        assertEquals(ErrorCode.QUESTION_NOT_FOUND, exception.getErrorCode());
        verify(projectApplicationRepository, never()).save(any());
    }

    @Test
    void deletesAnswersBeforeCancelingApplication() {
        when(applicant.getUserId()).thenReturn(2L);
        when(recruitment.getApplicantCount()).thenReturn(1);
        ProjectApplication application = ProjectApplication.builder()
                .user(applicant)
                .recruitment(recruitment)
                .status(ApplicationStatus.PENDING)
                .build();
        when(projectApplicationRepository.findById(20L)).thenReturn(Optional.of(application));

        projectApplicationService.deleteApplication(
                2L,
                DeleteProjectRequestDTO.builder().applicationId(20L).build()
        );

        InOrder order = inOrder(applicationAnswerRepository, projectApplicationRepository);
        order.verify(applicationAnswerRepository).deleteAllByApplication(application);
        order.verify(projectApplicationRepository).delete(application);
        verify(recruitment).setApplicantCount(0);
    }

    @Test
    void rejectsAnotherApplicationToTheSameProject() {
        User author = org.mockito.Mockito.mock(User.class);
        when(projectRecruitmentRepository.findById(10L)).thenReturn(Optional.of(recruitment));
        when(recruitment.getProject()).thenReturn(project);
        when(project.isRecruitmentClosed(clock)).thenReturn(false);
        when(project.getAuthor()).thenReturn(author);
        when(author.getUserId()).thenReturn(1L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(applicant));
        when(applicant.getUserId()).thenReturn(2L);
        when(projectApplicationRepository.existsByUserAndProject(applicant, project))
                .thenReturn(true);

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> projectApplicationService.applyProject(
                        2L,
                        ApplyProjectRequestDTO.builder()
                                .recruitmentId(10L)
                                .letter("지원합니다.")
                                .answers(List.of())
                                .build())
        );

        assertEquals(ErrorCode.ALREADY_APPLIED, exception.getErrorCode());
        verify(projectApplicationRepository, never()).save(any());
    }

    @Test
    void rejectedApplicationCannotBeCanceledAndReapplied() {
        when(applicant.getUserId()).thenReturn(2L);
        ProjectApplication application = ProjectApplication.builder()
                .user(applicant)
                .recruitment(recruitment)
                .status(ApplicationStatus.REJECTED)
                .build();
        when(projectApplicationRepository.findById(20L)).thenReturn(Optional.of(application));

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> projectApplicationService.deleteApplication(
                        2L,
                        DeleteProjectRequestDTO.builder().applicationId(20L).build())
        );

        assertEquals(ErrorCode.APPLICATION_ALREADY_PROCESSED, exception.getErrorCode());
        verify(projectApplicationRepository, never()).delete(any());
    }

    private void stubApplyContext() {
        User author = org.mockito.Mockito.mock(User.class);

        when(projectRecruitmentRepository.findById(10L)).thenReturn(Optional.of(recruitment));
        when(recruitment.getProject()).thenReturn(project);
        when(project.isRecruitmentClosed(clock)).thenReturn(false);
        when(project.getAuthor()).thenReturn(author);
        when(author.getUserId()).thenReturn(1L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(applicant));
        when(applicant.getUserId()).thenReturn(2L);
        when(projectApplicationRepository.existsByUserAndProject(applicant, project))
                .thenReturn(false);
    }

    private ProjectApplication stubStatusUpdateContext() {
        ProjectApplication application = org.mockito.Mockito.mock(ProjectApplication.class);
        User author = org.mockito.Mockito.mock(User.class);

        when(projectApplicationRepository.findById(20L)).thenReturn(Optional.of(application));
        when(application.getId()).thenReturn(20L);
        when(application.getRecruitment()).thenReturn(recruitment);
        when(application.getProject()).thenReturn(project);
        when(application.getStatus()).thenReturn(ApplicationStatus.PENDING);
        when(application.getUser()).thenReturn(applicant);
        when(recruitment.getProject()).thenReturn(project);
        when(project.getAuthor()).thenReturn(author);
        when(project.getTitle()).thenReturn("테스트 프로젝트");
        when(project.getStatus()).thenReturn(com.hicct3.projectfinder.entity.enums.ProjectStatus.RECRUITING);
        when(author.getUserId()).thenReturn(1L);
        when(applicant.getUserId()).thenReturn(2L);

        return application;
    }

    private ProjectQuestion question(
            Long id,
            QuestionType type,
            boolean required,
            String options,
            int orderIndex) {
        return ProjectQuestion.builder()
                .id(id)
                .project(project)
                .type(type)
                .label("질문 " + id)
                .options(options)
                .required(required)
                .orderIndex(orderIndex)
                .build();
    }

    private AnswerRequestDTO answer(Long questionId, String answerText) {
        return AnswerRequestDTO.builder()
                .questionId(questionId)
                .answerText(answerText)
                .build();
    }
}
