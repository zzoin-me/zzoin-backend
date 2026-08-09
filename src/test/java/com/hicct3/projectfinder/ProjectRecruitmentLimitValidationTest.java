package com.hicct3.projectfinder;

import com.hicct3.projectfinder.dto.project.CreateProjectRequestDTO;
import com.hicct3.projectfinder.dto.project.UpdateProjectRequestDTO;
import com.hicct3.projectfinder.dto.project.UpdateRecruitmentRequestDTO;
import com.hicct3.projectfinder.dto.project.recruitment.CreateRecruitmentRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectRecruitmentLimitValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createProjectAllowsSixRecruitmentsAndRejectsSeven() {
        CreateProjectRequestDTO request = CreateProjectRequestDTO.builder()
                .recruitments(createRecruitments(6))
                .build();

        assertThat(recruitmentViolations(request)).isEmpty();

        request.setRecruitments(createRecruitments(7));

        assertThat(recruitmentViolations(request)).hasSize(1);
    }

    @Test
    void updateProjectAllowsSixRecruitmentsAndRejectsSeven() {
        UpdateProjectRequestDTO request = UpdateProjectRequestDTO.builder()
                .recruitments(updateRecruitments(6))
                .build();

        assertThat(recruitmentViolations(request)).isEmpty();

        request.setRecruitments(updateRecruitments(7));

        assertThat(recruitmentViolations(request)).hasSize(1);
    }

    private List<CreateRecruitmentRequestDTO> createRecruitments(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> CreateRecruitmentRequestDTO.builder()
                        .jobRoleId((long) index + 1)
                        .recruitmentCount(1)
                        .qualification("자격 요건")
                        .preferred("우대 사항")
                        .build())
                .toList();
    }

    private List<UpdateRecruitmentRequestDTO> updateRecruitments(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> UpdateRecruitmentRequestDTO.builder()
                        .jobRoleId((long) index + 1)
                        .recruitmentCount(1)
                        .qualification("자격 요건")
                        .preferred("우대 사항")
                        .build())
                .toList();
    }

    private List<String> recruitmentViolations(Object request) {
        return validator.validate(request).stream()
                .filter(violation -> violation.getPropertyPath().toString().equals("recruitments"))
                .map(violation -> violation.getMessage())
                .toList();
    }
}
