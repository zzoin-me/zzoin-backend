package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.univ.UnivInfoListResponseDTO;
import com.hicct3.projectfinder.dto.univ.UnivInfoResponseDTO;
import com.hicct3.projectfinder.dto.user.UserProfileResponseDTO;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.UnivRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnivService {
    private final UnivRepository univRepository;

    @Transactional
    public UnivInfoListResponseDTO getUnivInfoList() {

        List<UnivInfoResponseDTO> list = univRepository.findAll().stream()
                .map(u -> UnivInfoResponseDTO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .domain(u.getDomain())
                        .build()).toList();

        return UnivInfoListResponseDTO.builder()
                .univInfoList(list)
                .build();
    }

    @Transactional
    public UnivInfoResponseDTO getUnivById(Long univId) {
        var univ = univRepository.findById(univId).orElseThrow(() -> new GeneralException(ErrorCode.UNIVERSITY_NOT_FOUND));
        return UnivInfoResponseDTO.builder()
                .id(univ.getId())
                .name(univ.getName())
                .domain(u.getDomain())
                .build();
    }
}
