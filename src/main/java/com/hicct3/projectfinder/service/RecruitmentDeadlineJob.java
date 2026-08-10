package com.hicct3.projectfinder.service;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

@DisallowConcurrentExecution
public class RecruitmentDeadlineJob extends QuartzJobBean {

    public static final String PROJECT_ID = "projectId";

    @Autowired
    private ProjectDeadlineService projectDeadlineService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        long projectId = context.getMergedJobDataMap().getLong(PROJECT_ID);
        projectDeadlineService.closeIfExpired(projectId);
    }
}
