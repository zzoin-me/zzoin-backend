package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.NotificationType;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProjectDeadlineService {

    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    @Transactional
    public boolean closeIfExpired(Long projectId) {
        Project project = projectRepository.findByIdForUpdate(projectId).orElse(null);
        if (project == null
                || project.getDeletedAt() != null
                || project.getStatus() != ProjectStatus.RECRUITING
                || project.getRecruitmentDeadline().isAfter(LocalDateTime.now(clock))) {
            return false;
        }

        project.setStatus(ProjectStatus.RECRUITMENT_CLOSED);
        project.setUpdatedAt(LocalDateTime.now(clock));

        User author = project.getAuthor();
        if (!notificationService.isAlreadyNotified(
                author,
                NotificationType.DEADLINE_REACHED,
                project.getId())) {
            notificationService.createNotification(
                    author.getUserId(),
                    NotificationType.DEADLINE_REACHED,
                    "프로젝트 모집이 마감되었어요",
                    "'" + project.getTitle() + "' 프로젝트의 모집 기한이 종료되었습니다. 진행 상태를 변경해주세요.",
                    "/projects/" + project.getId() + "/manage",
                    project.getId());
        }

        return true;
    }
}
