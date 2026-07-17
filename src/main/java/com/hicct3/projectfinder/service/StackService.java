package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.stack.StackInfoListResponseDTO;
import com.hicct3.projectfinder.dto.stack.StackInfoResponseDTO;
import com.hicct3.projectfinder.dto.stack.admin.CreateStackRequestDTO;
import com.hicct3.projectfinder.dto.univ.UnivInfoListResponseDTO;
import com.hicct3.projectfinder.dto.univ.UnivInfoResponseDTO;
import com.hicct3.projectfinder.entity.Stack;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.StackRepository;
import com.hicct3.projectfinder.repository.UnivRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StackService {
    private final StackRepository stackRepository;

    @Transactional
    public StackInfoListResponseDTO getStackInfoList() {

        List<StackInfoResponseDTO> list = stackRepository.findAll().stream()
                .map(u -> StackInfoResponseDTO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .build()).toList();

        return StackInfoListResponseDTO.builder()
                .stackInfoList(list)
                .build();
    }

    @Transactional
    public StackInfoResponseDTO getStackById(Long stackId) {
        var stack = stackRepository.findById(stackId).orElseThrow(() -> new GeneralException(ErrorCode.STACK_NOT_FOUND));
        return StackInfoResponseDTO.builder()
                .id(stack.getId())
                .name(stack.getName())
                .build();
    }

    @Transactional
    public void createStack(CreateStackRequestDTO req)
    {
        if(stackRepository.existsByName(req.getName()))
        {
            throw new GeneralException(ErrorCode.STACK_ALREADY_EXISTS);
        }

        stackRepository.save(Stack.builder()
                .name(req.getName())
                .build());
    }
}

