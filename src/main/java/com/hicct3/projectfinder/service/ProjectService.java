package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.project.*;
import com.hicct3.projectfinder.dto.project.myproject.MyApplicationPreviewResponseDTO;
import com.hicct3.projectfinder.dto.project.myproject.MyProjectPreviewResponseDTO;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.enums.*;
import com.hicct3.projectfinder.dto.project.review.*;
import com.hicct3.projectfinder.entity.*;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MemberReviewRepository memberReviewRepository;
    private final JobRoleRepository jobRoleRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final UserRepository userRepository;
    private final ProjectQuestionRepository projectQuestionRepository;
    private final RecruitmentService recruitmentService;

    @Transactional
    public void createReview(Long userId, Long projectId, CreateReviewRequestDTO req) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 프로젝트가 종료되었는지
        if (project.getStatus() != ProjectStatus.COMPLETED) {
            throw new GeneralException(ErrorCode.PROJECT_NOT_COMPLETED);
        }

        // 작성자가 프로젝트 멤버인지
        if (!projectMemberRepository.existsByUserAndProject(user, project)) {
            throw new GeneralException(ErrorCode.PROJECT_MEMBER_NOT_FOUND);
        }

        // 이미 평가를 작성했는지
        if (memberReviewRepository.existsByAuthorAndProject(user, project)) {
            throw new GeneralException(ErrorCode.ALREADY_REVIEWED);
        }

        List<Long> targetIds = req.getMembers().stream()
                .map(CreateMemberReviewRequestDTO::getUserId)
                .toList();

        // 자기 자신 평가 금지
        if (targetIds.contains(userId)) {
            throw new GeneralException(ErrorCode.CANNOT_REVIEW_SELF);
        }

        // 중복 평가 대상
        if (targetIds.size() != new HashSet<>(targetIds).size()) {
            throw new GeneralException(ErrorCode.USER_ID_DUPLICATE);
        }

        // 프로젝트 멤버 조회
        List<ProjectMember> projectMembers = projectMemberRepository.findAllByProject(project);

        // 자기 자신을 제외한 모든 프로젝트 멤버 ID
        Set<Long> expectedTargetIds = projectMembers.stream()
                .map(pm -> pm.getUser().getUserId())
                .filter(id -> !id.equals(userId))
                .collect(Collectors.toSet());

        Set<Long> requestTargetIds = new HashSet<>(targetIds);

        // 일부만 평가하거나, 프로젝트 외 유저를 포함한 경우
        if (!expectedTargetIds.equals(requestTargetIds)) {
            throw new GeneralException(ErrorCode.REVIEW_TARGET_INVALID);
        }

        // 평가 대상 유저 조회
        Map<Long, User> targetUsers = userRepository.findAllById(targetIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        if (targetUsers.size() != targetIds.size()) {
            throw new GeneralException(ErrorCode.USER_NOT_FOUND);
        }

        List<MemberReview> reviews = req.getMembers().stream()
                .map(member -> MemberReview.builder()
                        .contribution(member.getContribution())
                        .participation(member.getParticipation())
                        .responsibility(member.getResponsibility())
                        .comment(member.getComment())
                        .author(user)
                        .target(targetUsers.get(member.getUserId()))
                        .project(project)
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();

        memberReviewRepository.saveAll(reviews);
    }

    //해당 프로젝트의 맴버 목록 조회
    @Transactional
    public MembersResponseDTO getMembers(Long userId, Long projectId)
    {
        var project = projectRepository.findById(projectId).orElseThrow(()->new GeneralException(ErrorCode.PROJECT_NOT_FOUND));
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));

        //user가 project에 참여중인지 확인
        if(!projectMemberRepository.existsByUserAndProject(user, project))
            throw new GeneralException(ErrorCode.USER_NOT_IN_PROJECT);

        //해당 프로젝트의 팀원 목록 조회해서 반환
        return MembersResponseDTO.builder()
                .members(
                        projectMemberRepository.findAllByProject(project)
                                .stream()
                                .map(MemberResponseDTO::from)
                                .toList()
                )
                .build();
    }

    @Transactional
    public Page<MyReviewableProjectResponseDTO> getMyReviewableProjects(
            Long userId,
            Pageable pageable
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        Page<ProjectMember> members = projectMemberRepository
                .findByUserAndProject_Status(
                        user,
                        ProjectStatus.COMPLETED,
                        pageable
                );

        return members.map(member -> {

            Project project = member.getProject();

            long reviewedCount =
                    memberReviewRepository.countByAuthorAndProject(user, project);

            long otherMemberCount =
                    projectMemberRepository.countByProjectAndUserNot(project, user);

            boolean reviewCompleted = reviewedCount >= otherMemberCount;

            return MyReviewableProjectResponseDTO.builder()
                    .projectId(project.getId())
                    .title(project.getTitle())
                    .recruitment(member.getJobName())
                    .joinedAt(member.getJoinedAt())
                    .completedAt(member.getCompletedAt())
                    .reviewCompleted(reviewCompleted)
                    .build();
        });
    }

    //내가 작성한 팀원 평가 조회
    @Transactional
    public MemberReviewsResponseDTO getMyReviews(Long userId, Long projectId)
    {
        var project = projectRepository.findById(projectId).orElseThrow(()->new GeneralException(ErrorCode.PROJECT_NOT_FOUND));
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));

        //user가 project에 참여중인지 확인
        if(!projectMemberRepository.existsByUserAndProject(user, project))
            throw new GeneralException(ErrorCode.USER_NOT_IN_PROJECT);

        Map<User, List<String>> recruitmentMap = projectMemberRepository
                .findAllByProject(project)
                .stream()
                .collect(Collectors.groupingBy(
                        ProjectMember::getUser,
                        Collectors.mapping(ProjectMember::getJobName, Collectors.toList())
                ));

        List<MemberReviewResponseDTO> result = memberReviewRepository
                .findAllByAuthorAndProject(user, project)
                .stream()
                .map(memberReview -> MemberReviewResponseDTO.builder()
                        .memberId(memberReview.getId())
                        .nickname(memberReview.getTarget().getNickName())
                        .recruitments(recruitmentMap.get(memberReview.getTarget()))
                        .profileUrl(memberReview.getTarget().getProfileUrl())
                        .contribution(memberReview.getContribution())
                        .responsibility(memberReview.getResponsibility())
                        .participation(memberReview.getParticipation())
                        .build())
                .toList();

        return MemberReviewsResponseDTO.builder()
                .members(result)
                .build();

    }

    // 내가 받은 평가 조회
    @Transactional
    public MyReviewsResponseDTO getReceivedReviews(Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        var reviews = memberReviewRepository.findAllByTarget(user);

        int[] scores = new int[6];
        int totalScore = 0;
        int totalCount = 0;

        // 점수 집계
        for (MemberReview review : reviews) {
            scores[review.getContribution()]++;
            scores[review.getParticipation()]++;
            scores[review.getResponsibility()]++;

            totalScore += review.getContribution();
            totalScore += review.getParticipation();
            totalScore += review.getResponsibility();

            totalCount += 3;
        }

        double averageScore = totalCount == 0
                ? 0.0
                : Math.round((double) totalScore / totalCount * 10) / 10.0; // 소수 첫째 자리

        var reviewDtos = reviews.stream()
                .map(review -> MyReviewResponseDTO.builder()
                        .projectName(review.getProject().getTitle())
                        .comment(review.getComment())
                        .contribution(review.getContribution())
                        .responsibility(review.getResponsibility())
                        .participation(review.getParticipation())
                        .createdAt(review.getCreatedAt())
                        .avgRating(review.getAverage())
                        .build())
                .toList();

        return MyReviewsResponseDTO.builder()
                .reviews(reviewDtos)
                .ratingAvg(averageScore)   // DTO에 필드 추가 필요
                .score1(scores[1])
                .score2(scores[2])
                .score3(scores[3])
                .score4(scores[4])
                .score5(scores[5])
                .build();
    }

    @Transactional
    public Page<MyProjectPreviewResponseDTO> getMyProjects(
            Long userId,
            String status,
            boolean hasApplicants,
            Pageable pageable)
    {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        String normalizedStatus = "RECRUITING".equals(status) || "CLOSED".equals(status)
                ? status
                : null;

        return projectRepository.findMyProjects(user, normalizedStatus, hasApplicants, pageable)
                .map(project -> MyProjectPreviewResponseDTO.from(
                        project,
                        projectRecruitmentRepository.findAllByProjectAndDeletedAtIsNull(project)
                ));
    }

    @Transactional
    public Page<MyApplicationPreviewResponseDTO> getMyApplications(Long userId, ApplicationStatus status, Pageable pageable)
    {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        var applications = status != null
                ? projectApplicationRepository.findAllByUserAndStatus(user, status, pageable)
                : projectApplicationRepository.findAllByUser(user, pageable);

        return applications.map(MyApplicationPreviewResponseDTO::from);
    }

    //프로젝트 생성
    @Transactional
    public void createProject(Long userId, CreateProjectRequestDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        if (req.getRecruitments() == null || req.getRecruitments().isEmpty()) {
            throw new GeneralException(ErrorCode.RECRUITMENT_EMPTY);
        }

        //프로젝트 엔티티 생성
        Project savedProject = projectRepository.save(Project.create(req, user));

        //직군 저장
        recruitmentService.createRecruitments(savedProject, req.getRecruitments());

        createQuestions(savedProject, req.getQuestions());

        //생성자를 멤버로 할당
        var member = ProjectMember.builder()
                .status(MemberStatus.IN_PROGRESS)
                .user(user)
                .project(savedProject)
                .role(Role.OWNER)
                .joinedAt(LocalDateTime.now())
                .build();

        projectMemberRepository.save(member);

    }

    private void createQuestions(Project project, List<CreateQuestionRequestDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        List<ProjectQuestion> questions = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            CreateQuestionRequestDTO request = requests.get(i);
            List<String> options = request.getOptions() == null
                    ? Collections.emptyList()
                    : request.getOptions().stream()
                            .map(String::trim)
                            .filter(option -> !option.isEmpty())
                            .toList();

            if (request.getType() != QuestionType.TEXT && options.size() < 2) {
                throw new GeneralException("선택형 질문은 선택지를 2개 이상 등록해야 합니다.");
            }

            questions.add(ProjectQuestion.builder()
                    .project(project)
                    .orderIndex(i)
                    .type(request.getType())
                    .label(request.getLabel().trim())
                    .options(request.getType() == QuestionType.TEXT ? null : String.join(",", options))
                    .required(request.getRequired())
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        projectQuestionRepository.saveAll(questions);
    }



    //프로젝트 수정
    @Transactional
    public void updateProject(Long userId, Long projectId, UpdateProjectRequestDTO req)
    {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));
        var project = projectRepository.findById(projectId).orElseThrow(()->new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        if(project.getDeletedAt() != null)
        {
            throw new GeneralException(ErrorCode.PROJECT_DELETED);
        }

        if(!project.getAuthor().getUserId().equals(user.getUserId()))
            throw new GeneralException(ErrorCode.AUTHOR_MISMATCHED);

        project.setUpdatedAt(LocalDateTime.now());

        if(req.getTitle() != null)
            project.setTitle(req.getTitle());

        if(req.getDescription() != null)
            project.setDescription(req.getDescription());

        if(req.getCollaborationType() != null)
            project.setCollaborationType(req.getCollaborationType());

        if(req.getCommunicationTool() != null)
            project.setCommunicationTool(req.getCommunicationTool());

        if(req.getMeetingSchedule() != null)
            project.setMeetingSchedule(req.getMeetingSchedule());

        if(req.getPeriod() != null)
            project.setPeriod(req.getPeriod());

        if(req.getRecruitmentDeadline() != null)
            project.setRecruitmentDeadline(req.getRecruitmentDeadline());

        if(req.getGoalType() != null)
            project.setGoal(req.getGoalType());

        if(req.getImageUrl() != null)
            project.setImageUrl(req.getImageUrl());

        if(req.getRecruitments() != null)
        {
            recruitmentService.updateRecruitments(project, req.getRecruitments());
        }
    }


    //프로젝트 제거
    @Transactional
    public void deleteProject(Long userId, Long projectId)
    {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));
        var project = projectRepository.findById(projectId).orElseThrow(()->new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        if(project.getDeletedAt() != null)
            throw new GeneralException(ErrorCode.PROJECT_ALREADY_DELETED);

        if(!project.getAuthor().getUserId().equals(user.getUserId()))
            throw new GeneralException(ErrorCode.AUTHOR_MISMATCHED);

       project.setDeletedAt(LocalDateTime.now());
    }

    //프로젝트 상태 변경
    @Transactional
    public void setProjectStatus(Long userId, Long projectId, UpdateProjectStatusRequestDTO req)
    {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));
        var project = projectRepository.findById(projectId).orElseThrow(()->new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        if(project.getStatus().equals(ProjectStatus.COMPLETED))
            throw new GeneralException(ErrorCode.PROJECT_ALREADY_COMPLETED);

        if(project.getDeletedAt() != null)
        {
            throw new GeneralException(ErrorCode.PROJECT_DELETED);
        }

        if(!project.getAuthor().getUserId().equals(user.getUserId()))
            throw new GeneralException(ErrorCode.AUTHOR_MISMATCHED);

        if(req.getStatus().equals(ProjectStatus.COMPLETED))
        {
            for (ProjectMember projectMember : projectMemberRepository.findAllByProject(project)) {
                projectMember.setCompletedAt(LocalDateTime.now());
                projectMember.setStatus(MemberStatus.COMPLETED);
            }
        }

        project.setStatus(req.getStatus());
    }

}
