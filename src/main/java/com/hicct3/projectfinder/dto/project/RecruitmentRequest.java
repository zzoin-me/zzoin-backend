package com.hicct3.projectfinder.dto.project;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public interface RecruitmentRequest {
    Long getJobRoleId();
    String getCustomJobRoleName();
    Integer getRecruitmentCount();
    String getQualification();
    String getPreferred();
}
