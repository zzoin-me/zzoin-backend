package com.hicct3.projectfinder.global;

import com.hicct3.projectfinder.entity.JobCategory;
import com.hicct3.projectfinder.entity.JobRole;
import com.hicct3.projectfinder.entity.enums.JobCategoryCode;
import com.hicct3.projectfinder.repository.JobCategoryRepository;
import com.hicct3.projectfinder.repository.JobRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobCategoryInitializer implements CommandLineRunner {

    private final JobCategoryRepository jobCategoryRepository;
    private final JobRoleRepository jobRoleRepository;

    @Override
    public void run(String... args) {

        if (jobCategoryRepository.count() > 0) {
            return;
        }

        var planning = jobCategoryRepository.save(createCategory(JobCategoryCode.PLANNING));
        var design = jobCategoryRepository.save(createCategory(JobCategoryCode.DESIGN));
        var development = jobCategoryRepository.save(createCategory(JobCategoryCode.DEVELOPMENT));
        var marketing = jobCategoryRepository.save(createCategory(JobCategoryCode.MARKETING));
        var other = jobCategoryRepository.save(createCategory(JobCategoryCode.ETC));

        // 기획
        jobRoleRepository.save(createJobRole("서비스 기획", planning));
        jobRoleRepository.save(createJobRole("PM", planning));
        jobRoleRepository.save(createJobRole("프로젝트 매니저", planning));
        jobRoleRepository.save(createJobRole("사업 기획", planning));

        // 디자인
        jobRoleRepository.save(createJobRole("UX", design));
        jobRoleRepository.save(createJobRole("UI", design));
        jobRoleRepository.save(createJobRole("UX/UI", design));
        jobRoleRepository.save(createJobRole("그래픽", design));
        jobRoleRepository.save(createJobRole("브랜드", design));
        jobRoleRepository.save(createJobRole("일러스트", design));

        // 개발
        jobRoleRepository.save(createJobRole("프론트엔드", development));
        jobRoleRepository.save(createJobRole("백엔드", development));
        jobRoleRepository.save(createJobRole("iOS", development));
        jobRoleRepository.save(createJobRole("안드로이드", development));
        jobRoleRepository.save(createJobRole("크로스플랫폼", development));
        jobRoleRepository.save(createJobRole("데스크탑", development));
        jobRoleRepository.save(createJobRole("게임 클라이언트", development));
        jobRoleRepository.save(createJobRole("게임 서버", development));
        jobRoleRepository.save(createJobRole("DevOps", development));
        jobRoleRepository.save(createJobRole("데이터 엔지니어링", development));
        jobRoleRepository.save(createJobRole("보안", development));

        // 마케팅
        jobRoleRepository.save(createJobRole("콘텐츠", marketing));
        jobRoleRepository.save(createJobRole("성장", marketing));
        jobRoleRepository.save(createJobRole("SNS", marketing));
        jobRoleRepository.save(createJobRole("브랜드", marketing));
        jobRoleRepository.save(createJobRole("광고", marketing));
        jobRoleRepository.save(createJobRole("PR", marketing));
    }

    private JobCategory createCategory(JobCategoryCode code) {
        return JobCategory.builder()
                .categoryCode(code)
                .name(code.getName())
                .build();
    }

    private JobRole createJobRole(String name, JobCategory jobCategory) {
        return JobRole.builder()
                .name(name)
                .jobCategory(jobCategory)
                .isCustom(false)
                .build();
    }
}