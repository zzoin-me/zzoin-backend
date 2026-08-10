package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.deadline.recovery.enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class ProjectDeadlineRecovery {

    private final ProjectRepository projectRepository;
    private final ProjectDeadlineService projectDeadlineService;
    private final ProjectDeadlineScheduler projectDeadlineScheduler;
    private final Clock clock;

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileDeadlineJobs() {
        List<Project> recruitingProjects = projectRepository
                .findAllByStatusAndDeletedAtIsNull(ProjectStatus.RECRUITING);
        LocalDateTime now = LocalDateTime.now(clock);

        for (Project project : recruitingProjects) {
            if (!project.getRecruitmentDeadline().isAfter(now)) {
                projectDeadlineService.closeIfExpired(project.getId());
            } else {
                projectDeadlineScheduler.schedule(project.getId(), project.getRecruitmentDeadline());
            }
        }

        log.info("Reconciled {} project deadline jobs", recruitingProjects.size());
    }
}
