package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.project.RecruitmentRequest;
import com.hicct3.projectfinder.dto.project.UpdateRecruitmentRequestDTO;
import com.hicct3.projectfinder.dto.project.recruitment.CreateRecruitmentRequestDTO;
import com.hicct3.projectfinder.entity.JobCategory;
import com.hicct3.projectfinder.entity.JobRole;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectRecruitment;
import com.hicct3.projectfinder.entity.enums.JobCategoryCode;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class RecruitmentService {
    private final ProjectRepository projectRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MemberReviewRepository memberReviewRepository;
    private final JobRoleRepository jobRoleRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createRecruitments(Project project, List<CreateRecruitmentRequestDTO> list) {

        RecruitmentContext context = prepareContext(list);

        List<ProjectRecruitment> recruitments = list.stream().map(dto -> createRecruitment(project, dto, resolveJobRole(dto, context))).toList();

        projectRecruitmentRepository.saveAll(recruitments);
    }

    @Transactional
    public void updateRecruitments(Project project, List<UpdateRecruitmentRequestDTO> list) {

        RecruitmentContext context = prepareContext(list);

        Map<Long, ProjectRecruitment> recruitments = projectRecruitmentRepository.findAllByProject(project).stream().collect(Collectors.toMap(ProjectRecruitment::getId, Function.identity()));

        Set<Long> remainingRecruitmentIds = new HashSet<>();
        List<ProjectRecruitment> newRecruitments = new ArrayList<>();

        for (UpdateRecruitmentRequestDTO dto : list) {

            JobRole jobRole = resolveJobRole(dto, context);

            //기존 recruitment 수정
            if (dto.getRecruitmentId() != null) {

                ProjectRecruitment recruitment = Optional.ofNullable(recruitments.get(dto.getRecruitmentId())).orElseThrow(() -> new GeneralException(ErrorCode.RECRUITMENT_NOT_FOUND));

                recruitment.update(jobRole, dto.getRecruitmentCount(), dto.getQualification(), dto.getPreferred());

                remainingRecruitmentIds.add(recruitment.getId());

            } else {
                //recruitment 추가
                newRecruitments.add(createRecruitment(project, dto, jobRole));
            }
        }

        recruitments.values().stream().filter(r -> !remainingRecruitmentIds.contains(r.getId())).forEach(ProjectRecruitment::delete);

        projectRecruitmentRepository.saveAll(newRecruitments);
    }

    //recruitments 검증
    //1. 중복되는 jobRoleId가 있는가
    //2. jobRoleId와 customJobRole이 모두 있는가
    //3. jobRoleId와 customJobRole이 둘다 없는가
    private Set<Long> validateAndCollectJobRoles(List<? extends RecruitmentRequest> recruitments) {
        Set<String> customJobRoles = new HashSet<>();
        Set<Long> jobRoleIds = new HashSet<>();

        for (RecruitmentRequest recruitment : recruitments) {
            boolean hasJobRole = recruitment.getJobRoleId() != null;
            boolean hasCustomJobRole = StringUtils.hasText(recruitment.getCustomJobRoleName());

            // 공식 직군 + 커스텀 직군 동시 입력 예외
            if (hasJobRole && hasCustomJobRole) {
                throw new GeneralException(ErrorCode.INVALID_JOB_ROLE);
            }

            // 둘 다 미입력 예외
            if (!hasJobRole && !hasCustomJobRole) {
                throw new GeneralException(ErrorCode.CUSTOM_JOB_ROLE_REQUIRED);
            }

            if (hasJobRole) {
                if (!jobRoleIds.add(recruitment.getJobRoleId())) {
                    throw new GeneralException(ErrorCode.ROLE_DUPLICATE);
                }
            } else {
                String customJobRole = recruitment.getCustomJobRoleName().trim();
                if (!customJobRoles.add(customJobRole)) {
                    throw new GeneralException(ErrorCode.RECRUITMENT_DUPLICATE);
                }
            }
        }

        return jobRoleIds;
    }

    //공식 직군 일괄 조회
    private Map<Long, JobRole> fetchOfficialRoles(Set<Long> jobRoleIds) {
        if (jobRoleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, JobRole> roles = jobRoleRepository.findAllById(jobRoleIds).stream().collect(Collectors.toMap(JobRole::getId, Function.identity()));

        if (roles.size() != jobRoleIds.size()) {
            throw new GeneralException(ErrorCode.JOB_ROLE_NOT_FOUND);
        }

        return roles;
    }

    @Getter
    @AllArgsConstructor
    private static class RecruitmentContext {
        private final Map<Long, JobRole> officialRoles;
        private final JobCategory etcCategory;
    }

    private RecruitmentContext prepareContext(List<? extends RecruitmentRequest> recruitments) {

        //커스텀을 제외한 jobRoleId 수집
        Set<Long> jobIds = validateAndCollectJobRoles(recruitments);

        //공식 직군 일괄 조회
        Map<Long, JobRole> officialRoles = fetchOfficialRoles(jobIds);

        JobCategory etcCategory = jobCategoryRepository.findByCategoryCode(JobCategoryCode.ETC).orElseThrow(() -> new GeneralException(ErrorCode.JOB_CATEGORY_NOT_FOUND));

        return new RecruitmentContext(officialRoles, etcCategory);
    }

    private JobRole resolveJobRole(RecruitmentRequest dto, RecruitmentContext context) {
        if (dto.getJobRoleId() != null) {
            return context.getOfficialRoles().get(dto.getJobRoleId());
        }

        return jobRoleRepository.save(new JobRole(dto.getCustomJobRoleName(), true, context.getEtcCategory()));
    }

    private ProjectRecruitment createRecruitment(Project project, RecruitmentRequest dto, JobRole jobRole) {
        return ProjectRecruitment.builder().project(project).jobRole(jobRole).applicantCount(0).recruitmentCount(dto.getRecruitmentCount()).qualification(dto.getQualification()).preferred(dto.getPreferred()).build();
    }

}

