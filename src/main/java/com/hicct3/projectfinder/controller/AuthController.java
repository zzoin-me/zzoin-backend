package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.dto.auth.*;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입 이메일 전송")
    @PostMapping("/signup/email/send")
    public ApiResponse<Void> sendSignupEmail(@RequestBody @Valid EmailSendRequestDTO req)
    {
        authService.sendSignupEmail(req.getEmail());

        return ApiResponse.onSuccess("회원가입 이메일 전송에 성공했습니다.", null);
    }

    @Operation(summary = "회원가입 이메일 인증")
    @PostMapping("/signup/email/verify")
    public ApiResponse<EmailVerifyResponseDTO> verifySignupEmail(@RequestBody @Valid EmailVerifyRequestDTO req)
    {
        return ApiResponse.onSuccess("회원가입 이메일 인증에 성공했습니다.", authService.verifySignupEmail(req));
    }

    @Operation(summary = "대학 이메일 전송")
    @PostMapping("/email/send")
    public ApiResponse<Void> sendEmail(Authentication auth, @RequestBody @Valid EmailSendRequestDTO req)
    {
        var userId = ((CustomUserDetails) auth.getPrincipal()).getId();
        authService.sendEmail(userId, req.getEmail());

        return ApiResponse.onSuccess("이메일 전송에 성공했습니다.", null);
    }

    @Operation(summary = "대학 이메일 인증")
    @PostMapping("/email/verify")
    public ApiResponse<EmailVerifyResponseDTO> verifyEmail(Authentication auth, @RequestBody @Valid UnivEmailVerifyRequestDTO req)
    {
        var userId = ((CustomUserDetails) auth.getPrincipal()).getId();
        authService.verifyEmail(userId, req);
        return ApiResponse.onSuccess("이메일 인증에 성공했습니다.", null);
    }

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ApiResponse<Void> signup(@RequestBody @Valid SignUpRequestDTO req)
    {
        authService.signUp(req);
        return ApiResponse.onSuccess("회원가입에 성공했습니다.", null);
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ApiResponse<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO req)
    {
        return ApiResponse.onSuccess("로그인에 성공했습니다.", authService.login(req));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication auth)
    {
        var userId = ((CustomUserDetails) auth.getPrincipal()).getId();
        authService.logout(userId);

        return ApiResponse.onSuccess("로그아웃에 성공했습니다.", null);
    }

    @Operation(summary = "토큰 갱신")
    @PostMapping("/refreshToken")
    public ApiResponse<RefreshTokenResponseDTO> refreshToken(@RequestBody @Valid RefreshTokenRequestDTO req)
    {
        return ApiResponse.onSuccess("토큰 갱신에 성공했습니다.", authService.refreshToken(req.getRefreshToken()));
    }
}
