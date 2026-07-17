package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.dto.univ.UnivInfoListResponseDTO;
import com.hicct3.projectfinder.dto.univ.UnivInfoResponseDTO;
import com.hicct3.projectfinder.dto.user.UserProfileResponseDTO;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.service.UnivService;
import com.hicct3.projectfinder.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/univ")
public class UnivController {
    private final UnivService univService;

    @GetMapping
    public ApiResponse<UnivInfoListResponseDTO> getUniversities()
    {
        return ApiResponse.onSuccess(univService.getUnivInfoList());
    }

    @GetMapping("/{univId}")
    public ApiResponse<UnivInfoResponseDTO> getUnivById(@PathVariable Long univId)
    {
        return ApiResponse.onSuccess(univService.getUnivById(univId));
    }

}
