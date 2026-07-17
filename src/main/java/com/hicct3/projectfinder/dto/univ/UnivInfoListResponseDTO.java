package com.hicct3.projectfinder.dto.univ;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class UnivInfoListResponseDTO {
    private List<UnivInfoResponseDTO> univInfoList;
}
