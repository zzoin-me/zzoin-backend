package com.hicct3.projectfinder.dto.stack;

import com.hicct3.projectfinder.dto.univ.UnivInfoResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class StackInfoListResponseDTO {
    private List<StackInfoResponseDTO> stackInfoList;
}
