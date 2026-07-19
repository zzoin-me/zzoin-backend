package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.project.CreateProjectRequestDTO;
import com.hicct3.projectfinder.dto.project.UpdateProjectRequestDTO;
import com.hicct3.projectfinder.dto.project.UpdateProjectStatusRequestDTO;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectRecruitment;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.ProjectRecruitmentRepository;
import com.hicct3.projectfinder.repository.ProjectRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createProject(Long userId, CreateProjectRequestDTO req)
    {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));

        if(req.getRecruitments().isEmpty())
            throw new GeneralException(ErrorCode.RECRUITMENT_EMPTY);

        var project = Project.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .collaborationType(req.getCollaborationType())
                .communicationTool(req.getCommunicationTool())
                .meetingSchedule(req.getMeetingSchedule())
                .period(req.getPeriod())
                .recruitmentDeadline(req.getRecruitmentDeadline())
                .goal(req.getGoalType())
                .imageUrl(req.getImageUrl())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .author(user)
                .status(ProjectStatus.RECRUITING)
                .build();


        req.getRecruitments().forEach(x->
                projectRecruitmentRepository.save(ProjectRecruitment.builder()
                        .name(x.getName())
                        .recruitmentCount(x.getCount())
                        .qualification(x.getQualification())
                        .preferred(x.getPreferred())
                        .project(project)
                        .build()));

        projectRepository.save(project);
    }

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
            if(req.getRecruitments().isEmpty())
                throw new GeneralException(ErrorCode.RECRUITMENT_EMPTY);

            projectRecruitmentRepository.deleteAllByProject(project);
            List<ProjectRecruitment> recruitments = req.getRecruitments().stream()
                    .map(x -> ProjectRecruitment.builder()
                            .name(x.getName())
                            .recruitmentCount(x.getCount())
                            .qualification(x.getQualification())
                            .preferred(x.getPreferred())
                            .project(project)
                            .build())
                    .toList();

            projectRecruitmentRepository.saveAll(recruitments);
        }
    }

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

    @Transactional
    public void setProjectStatus(Long userId, Long projectId, UpdateProjectStatusRequestDTO req)
    {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));
        var project = projectRepository.findById(projectId).orElseThrow(()->new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        if(project.getDeletedAt() != null)
        {
            throw new GeneralException(ErrorCode.PROJECT_DELETED);
        }

        if(!project.getAuthor().getUserId().equals(user.getUserId()))
            throw new GeneralException(ErrorCode.AUTHOR_MISMATCHED);

        project.setStatus(req.getStatus());
    }
}

