package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.project.RecruitmentRequest;
import com.hicct3.projectfinder.dto.project.UpdateRecruitmentRequestDTO;
import com.hicct3.projectfinder.dto.project.recruitment.CreateRecruitmentRequestDTO;
import com.hicct3.projectfinder.entity.JobRole;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectRecruitment;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    private final UserRepository userRepository;

    @Transactional
    public void createRecruitments(Project project, List<CreateRecruitmentRequestDTO> list) {

        Map<Long, JobRole> officialRoles = validateAndCollectJobRoles(list);

        List<ProjectRecruitment> recruitments = list.stream().map(dto -> createRecruitment(project, dto, officialRoles.get(dto.getJobRoleId()))).toList();

        projectRecruitmentRepository.saveAll(recruitments);
    }

    @Transactional
    public void updateRecruitments(Project project, List<UpdateRecruitmentRequestDTO> list) {

        Map<Long, JobRole> officialRoles = validateAndCollectJobRoles(list);

        Map<Long, ProjectRecruitment> recruitments = projectRecruitmentRepository.findAllByProject(project).stream().collect(Collectors.toMap(ProjectRecruitment::getId, Function.identity()));

        Set<Long> remainingRecruitmentIds = new HashSet<>();
        List<ProjectRecruitment> newRecruitments = new ArrayList<>();

        for (UpdateRecruitmentRequestDTO dto : list) {

            JobRole jobRole = officialRoles.get(dto.getJobRoleId());

            if (dto.getRecruitmentId() != null) {

                ProjectRecruitment recruitment = Optional.ofNullable(recruitments.get(dto.getRecruitmentId())).orElseThrow(() -> new GeneralException(ErrorCode.RECRUITMENT_NOT_FOUND));

                recruitment.update(jobRole, dto.getRecruitmentCount(), dto.getQualification(), dto.getPreferred());

                remainingRecruitmentIds.add(recruitment.getId());

            } else {
                newRecruitments.add(createRecruitment(project, dto, jobRole));
            }
        }

        recruitments.values().stream().filter(r -> !remainingRecruitmentIds.contains(r.getId())).forEach(ProjectRecruitment::delete);

        projectRecruitmentRepository.saveAll(newRecruitments);
    }

    private Map<Long, JobRole> validateAndCollectJobRoles(List<? extends RecruitmentRequest> recruitments) {
        Set<Long> jobRoleIds = new HashSet<>();

        for (RecruitmentRequest recruitment : recruitments) {
            if (recruitment.getJobRoleId() == null) {
                throw new GeneralException(ErrorCode.JOB_ROLE_NOT_FOUND);
            }
            if (!jobRoleIds.add(recruitment.getJobRoleId())) {
                throw new GeneralException(ErrorCode.ROLE_DUPLICATE);
            }
        }

        if (jobRoleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, JobRole> roles = jobRoleRepository.findAllById(jobRoleIds).stream().collect(Collectors.toMap(JobRole::getId, Function.identity()));

        if (roles.size() != jobRoleIds.size()) {
            throw new GeneralException(ErrorCode.JOB_ROLE_NOT_FOUND);
        }

        return roles;
    }

    private ProjectRecruitment createRecruitment(Project project, RecruitmentRequest dto, JobRole jobRole) {
        return ProjectRecruitment.builder().project(project).jobRole(jobRole).applicantCount(0).recruitmentCount(dto.getRecruitmentCount()).qualification(dto.getQualification()).preferred(dto.getPreferred()).build();
    }

}
