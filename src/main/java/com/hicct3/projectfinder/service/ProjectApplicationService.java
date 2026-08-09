package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.application.*;
import com.hicct3.projectfinder.dto.project.CreateProjectRequestDTO;
import com.hicct3.projectfinder.dto.project.UpdateProjectRequestDTO;
import com.hicct3.projectfinder.dto.project.UpdateProjectStatusRequestDTO;
import com.hicct3.projectfinder.entity.*;
import com.hicct3.projectfinder.entity.enums.ApplicationStatus;
import com.hicct3.projectfinder.entity.enums.MemberStatus;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.entity.enums.QuestionType;
import com.hicct3.projectfinder.entity.enums.Role;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectApplicationService {
    private final ProjectRepository projectRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectQuestionRepository projectQuestionRepository;
    private final ApplicationAnswerRepository applicationAnswerRepository;

    @Transactional
    public void updateApplicantStatus(Long userId, Long applicationId, UpdateApplicantStatusDTO dto)
    {
        var application = projectApplicationRepository.findById(applicationId).orElseThrow(() -> new GeneralException(ErrorCode.APPLICATION_NOT_FOUND));
        if(!application.getRecruitment().getProject().getAuthor().getUserId().equals(userId))
            throw new GeneralException(ErrorCode.AUTHOR_MISMATCHED);

        if(application.getStatus() != ApplicationStatus.PENDING)
            throw new GeneralException(ErrorCode.APPLICATION_ALREADY_PROCESSED);

        if(dto.getStatus() == ApplicationStatus.APPROVED)
        {
            ProjectMember member = ProjectMember.builder()
                    .status(MemberStatus.IN_PROGRESS)
                    .joinedAt(LocalDateTime.now())
                    .completedAt(null)
                    .user(application.getUser())
                    .project(application.getRecruitment().getProject())
                    .recruitment(application.getRecruitment())
                    .role(Role.MEMBER)
                    .build();
            projectMemberRepository.save(member);
        }

       application.setStatus(dto.getStatus());
    }

    @Transactional
    public ProjectApplicantsResponseDTO getApplicants(Long userId, Long projectId)
    {
        var user = userRepository.findById(userId).orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
        var project = projectRepository.findById(projectId).orElseThrow(() -> new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        if(!project.getAuthor().getUserId().equals(user.getUserId()))
            throw new GeneralException(ErrorCode.AUTHOR_MISMATCHED);

        List<ProjectApplication> applications = projectApplicationRepository.findAllByProject(project);
        List<ApplicationAnswer> answers = applications.isEmpty()
                ? Collections.emptyList()
                : applicationAnswerRepository.findAllByApplicationIn(applications);
        Map<Long, List<AnswerResponseDTO>> answersByApplicationId = answers
                .stream()
                .sorted(Comparator.comparing(answer -> answer.getQuestion().getOrderIndex()))
                .collect(Collectors.groupingBy(
                        answer -> answer.getApplication().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(AnswerResponseDTO::from, Collectors.toList())
                ));

        return ProjectApplicantsResponseDTO.of(applications.stream()
                .map(application -> ProjectApplicantResponseDTO.from(
                        application,
                        projectMemberRepository.findAllByUser(application.getUser()),
                        answersByApplicationId.getOrDefault(application.getId(), Collections.emptyList())
                ))
                .toList());
    }

    @Transactional
    public void applyProject(Long userId, ApplyProjectRequestDTO req) {
       var recruitment = projectRecruitmentRepository.findById(req.getRecruitmentId()).orElseThrow(() -> new GeneralException(ErrorCode.RECRUITMENT_NOT_FOUND));

       if(recruitment.getProject().isRecruitmentClosed())
           throw new GeneralException(ErrorCode.RECRUITMENT_CLOSED);

       var user = userRepository.findById(userId).orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

       if(recruitment.getProject().getAuthor().getUserId().equals(user.getUserId()))
           throw new GeneralException(ErrorCode.AUTHOR_NOT_APPLICABLE);

       //중복 지원 여부
       if(projectApplicationRepository.existsByUserAndRecruitment(user, recruitment))
           throw new GeneralException(ErrorCode.ALREADY_APPLIED);

       Map<ProjectQuestion, String> validatedAnswers = validateAnswers(
               recruitment.getProject(),
               req.getAnswers()
       );

       ProjectApplication application = ProjectApplication.builder()
               .user(user)
               .recruitment(recruitment)
               .letter(req.getLetter())
               .createdAt(LocalDateTime.now())
               .status(ApplicationStatus.PENDING)
               .build();

       recruitment.setApplicantCount(recruitment.getApplicantCount() + 1);

       projectApplicationRepository.save(application);

       if (!validatedAnswers.isEmpty()) {
           applicationAnswerRepository.saveAll(validatedAnswers.entrySet().stream()
                   .map(entry -> ApplicationAnswer.builder()
                           .application(application)
                           .question(entry.getKey())
                           .answerText(entry.getValue())
                           .build())
                   .toList());
       }
   }

   @Transactional
   public void deleteApplication(Long userId, DeleteProjectRequestDTO req) {
       var application = projectApplicationRepository.findById(req.getApplicationId()).orElseThrow(() -> new GeneralException(ErrorCode.APPLICATION_NOT_FOUND));

       if(!application.getUser().getUserId().equals(userId))
           throw new GeneralException(ErrorCode.AUTHOR_MISMATCHED);

       application.getRecruitment().setApplicantCount(application.getRecruitment().getApplicantCount() - 1);
       applicationAnswerRepository.deleteAllByApplication(application);
       projectApplicationRepository.delete(application);
   }

   private Map<ProjectQuestion, String> validateAnswers(
           Project project,
           List<AnswerRequestDTO> requests) {
       List<ProjectQuestion> questions = projectQuestionRepository
               .findAllByProjectAndDeletedAtIsNullOrderByIdAsc(project);
       Map<Long, ProjectQuestion> questionsById = questions.stream()
               .collect(Collectors.toMap(ProjectQuestion::getId, Function.identity()));
       Map<ProjectQuestion, String> validatedAnswers = new LinkedHashMap<>();
       Set<Long> answeredQuestionIds = new HashSet<>();

       for (AnswerRequestDTO request : Optional.ofNullable(requests).orElseGet(Collections::emptyList)) {
           if (!answeredQuestionIds.add(request.getQuestionId())) {
               throw new GeneralException(ErrorCode.QUESTION_ANSWER_DUPLICATE);
           }

           ProjectQuestion question = Optional.ofNullable(questionsById.get(request.getQuestionId()))
                   .orElseThrow(() -> new GeneralException(ErrorCode.QUESTION_NOT_FOUND));
           validatedAnswers.put(question, validateAnswer(question, request.getAnswerText()));
       }

       boolean hasMissingRequiredAnswer = questions.stream()
               .anyMatch(question -> Boolean.TRUE.equals(question.getRequired())
                       && !answeredQuestionIds.contains(question.getId()));

       if (hasMissingRequiredAnswer) {
           throw new GeneralException(ErrorCode.QUESTION_ANSWER_REQUIRED);
       }

       return validatedAnswers;
   }

   private String validateAnswer(ProjectQuestion question, String answerText) {
       String normalizedAnswer = answerText.trim();

       if (question.getType() == QuestionType.TEXT) {
           return normalizedAnswer;
       }

       if (question.getOptions() == null) {
           throw new GeneralException(ErrorCode.INVALID_QUESTION_ANSWER);
       }

       List<String> options = Arrays.stream(question.getOptions().split(","))
               .map(String::trim)
               .filter(option -> !option.isEmpty())
               .toList();

       if (question.getType() == QuestionType.SINGLE_CHOICE) {
           if (!options.contains(normalizedAnswer)) {
               throw new GeneralException(ErrorCode.INVALID_QUESTION_ANSWER);
           }
           return normalizedAnswer;
       }

       List<String> selections = Arrays.stream(normalizedAnswer.split(","))
               .map(String::trim)
               .filter(selection -> !selection.isEmpty())
               .toList();
       Set<String> distinctSelections = new LinkedHashSet<>(selections);

       if (distinctSelections.isEmpty()
               || distinctSelections.size() != selections.size()
               || !options.containsAll(distinctSelections)) {
           throw new GeneralException(ErrorCode.INVALID_QUESTION_ANSWER);
       }

       return options.stream()
               .filter(distinctSelections::contains)
               .collect(Collectors.joining(","));
   }
}
