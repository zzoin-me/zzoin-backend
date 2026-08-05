package com.hicct3.projectfinder.dto.project.review;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class MembersResponseDTO {
    private List<MemberResponseDTO> members;
}