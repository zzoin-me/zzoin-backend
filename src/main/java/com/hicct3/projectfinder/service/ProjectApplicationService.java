package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.application.*;
import com.hicct3.projectfinder.dto.project.CreateProjectRequestDTO;
import com.hicct3.projectfinder.dto.project.UpdateProjectRequestDTO;
import com.hicct3.projectfinder.dto.project.UpdateProjectStatusRequestDTO;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectApplication;
import com.hicct3.projectfinder.entity.ProjectMember;
import com.hicct3.projectfinder.entity.ProjectRecruitment;
import com.hicct3.projectfinder.entity.enums.ApplicationStatus;
import com.hicct3.projectfinder.entity.enums.MemberStatus;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectApplicationService {
    private final ProjectRepository projectRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

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
                    .build();
            projectMemberRepository.save(member);
        }

       application.getRecruitment().setApplicantCount(application.getRecruitment().getApplicantCount() - 1);
       application.setStatus(dto.getStatus());
    }

    @Transactional
    public ProjectApplicantsResponseDTO getApplicants(Long userId, Long projectId)
    {
        var user = userRepository.findById(userId).orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
        var project = projectRepository.findById(projectId).orElseThrow(() -> new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        if(!project.getAuthor().getUserId().equals(user.getUserId()))
            throw new GeneralException(ErrorCode.AUTHOR_MISMATCHED);

        return ProjectApplicantsResponseDTO.of(projectApplicationRepository.findAllByProject(project).stream().map(x->ProjectApplicantResponseDTO.from(x, projectMemberRepository.findAllByUser(x.getUser()))).toList());
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

       ProjectApplication application = ProjectApplication.builder()
               .user(user)
               .recruitment(recruitment)
               .letter(req.getLetter())
               .createdAt(LocalDateTime.now())
               .status(ApplicationStatus.PENDING)
               .build();

       recruitment.setApplicantCount(recruitment.getApplicantCount() + 1);

       projectApplicationRepository.save(application);
   }

   @Transactional
   public void deleteApplication(Long userId, DeleteProjectRequestDTO req) {
       var application = projectApplicationRepository.findById(req.getApplicationId()).orElseThrow(() -> new GeneralException(ErrorCode.APPLICATION_NOT_FOUND));

       if(!application.getUser().getUserId().equals(userId))
           throw new GeneralException(ErrorCode.AUTHOR_MISMATCHED);

       application.getRecruitment().setApplicantCount(application.getRecruitment().getApplicantCount() - 1);
       projectApplicationRepository.delete(application);
   }
}

