package com.hicct3.projectfinder.dto.user;

import com.hicct3.projectfinder.dto.stack.StackInfoResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserSchoolProfileResponseDTO {
    private String schoolName;
    private String major;
    private Integer grade;
}
