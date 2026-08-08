package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.NotificationType;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectStatusScheduler {

    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void autoCloseExpiredRecruitments() {
        LocalDateTime now = LocalDateTime.now();

        List<Project> expired = projectRepository.findAll().stream()
                .filter(p -> p.getDeletedAt() == null)
                .filter(p -> p.getStatus() == ProjectStatus.RECRUITING)
                .filter(p -> p.getRecruitmentDeadline().isBefore(now))
                .toList();

        if (expired.isEmpty()) return;

        log.info("Auto-closing {} expired recruitments", expired.size());

        for (Project project : expired) {
            project.setStatus(ProjectStatus.RECRUITMENT_CLOSED);

            User author = project.getAuthor();
            if (!notificationService.isAlreadyNotified(author, NotificationType.DEADLINE_REACHED, project.getId())) {
                notificationService.createNotification(
                        author.getUserId(),
                        NotificationType.DEADLINE_REACHED,
                        "프로젝트 모집이 마감되었어요",
                        "'" + project.getTitle() + "' 프로젝트의 모집 기한이 종료되었습니다. 진행 상태를 변경해주세요.",
                        "/projects/" + project.getId() + "/manage",
                        project.getId());
            }
        }
    }
}
