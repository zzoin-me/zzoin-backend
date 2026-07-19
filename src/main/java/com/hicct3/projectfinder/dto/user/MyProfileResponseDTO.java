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
public class MyProfileResponseDTO {
    private String name;
    private String email;
    private String field;
    private String bio;
    private String profileUrl;
    private Boolean verified;

    private List<StackInfoResponseDTO> stackInfoList;

}
