package com.hicct3.projectfinder.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectDeadlineScheduler {

    private static final String JOB_GROUP = "recruitment-deadline-jobs";
    private static final String TRIGGER_GROUP = "recruitment-deadline-triggers";

    private final Scheduler scheduler;
    private final Clock clock;

    public void scheduleAfterCommit(Long projectId, LocalDateTime deadline) {
        afterCommit(() -> schedule(projectId, deadline));
    }

    public void cancelAfterCommit(Long projectId) {
        afterCommit(() -> cancel(projectId));
    }

    public synchronized void schedule(Long projectId, LocalDateTime deadline) {
        JobKey jobKey = jobKey(projectId);
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }

            JobDetail jobDetail = JobBuilder.newJob(RecruitmentDeadlineJob.class)
                    .withIdentity(jobKey)
                    .usingJobData(RecruitmentDeadlineJob.PROJECT_ID, projectId)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey(projectId))
                    .forJob(jobDetail)
                    .startAt(Date.from(deadline.atZone(clock.getZone()).toInstant()))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow())
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new IllegalStateException("프로젝트 마감 작업을 예약하지 못했습니다.", e);
        }
    }

    public synchronized void cancel(Long projectId) {
        try {
            scheduler.deleteJob(jobKey(projectId));
        } catch (SchedulerException e) {
            throw new IllegalStateException("프로젝트 마감 작업을 취소하지 못했습니다.", e);
        }
    }

    private JobKey jobKey(Long projectId) {
        return JobKey.jobKey("recruitment-close-" + projectId, JOB_GROUP);
    }

    private TriggerKey triggerKey(Long projectId) {
        return TriggerKey.triggerKey("recruitment-close-" + projectId, TRIGGER_GROUP);
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runSafely(action);
                }
            });
            return;
        }
        runSafely(action);
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            log.error("Committed project change, but failed to synchronize its Quartz deadline job", exception);
        }
    }
}
