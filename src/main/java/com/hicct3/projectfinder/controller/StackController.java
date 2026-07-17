package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.dto.stack.StackInfoListResponseDTO;
import com.hicct3.projectfinder.dto.stack.StackInfoResponseDTO;
import com.hicct3.projectfinder.dto.stack.admin.CreateStackRequestDTO;
import com.hicct3.projectfinder.dto.univ.UnivInfoListResponseDTO;
import com.hicct3.projectfinder.dto.univ.UnivInfoResponseDTO;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.repository.StackRepository;
import com.hicct3.projectfinder.service.StackService;
import com.hicct3.projectfinder.service.UnivService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/stack")
public class StackController {
    private final StackService stackService;

    @GetMapping
    public ApiResponse<StackInfoListResponseDTO> getStacks()
    {
        return ApiResponse.onSuccess(stackService.getStackInfoList());
    }

    @GetMapping("/{stackId}")
    public ApiResponse<StackInfoResponseDTO> getStackById(@PathVariable Long stackId)
    {
        return ApiResponse.onSuccess(stackService.getStackById(stackId));
    }

    @PostMapping
    public ApiResponse<Void> createStack(@RequestBody CreateStackRequestDTO createStackRequestDTO)
    {
        stackService.createStack(createStackRequestDTO);
        return ApiResponse.onSuccess(null);
    }
}
