package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.dto.auth.*;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.service.AuthService;
import com.hicct3.projectfinder.service.OAuthAuthService;
import com.hicct3.projectfinder.service.SecurityRateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuthAuthService oAuthAuthService;
    private final SecurityRateLimitService rateLimitService;

    @Operation(summary = "소셜 계정 연결")
    @PostMapping("/link-account")
    public ApiResponse<LoginResponseDTO> linkAccount(
            HttpServletRequest request,
            @RequestBody @Valid LinkAccountRequestDTO req) {
        String rateKey = clientIp(request);
        Duration window = Duration.ofMinutes(15);
        rateLimitService.assertAllowed("oauth-link", rateKey, 5, window);
        Map<String, Object> result;
        try {
            result = oAuthAuthService.linkAccount(req.getTempToken(), req.getPassword());
            rateLimitService.reset("oauth-link", rateKey);
        } catch (GeneralException exception) {
            rateLimitService.record("oauth-link", rateKey, window);
            throw exception;
        }
        LoginResponseDTO dto = LoginResponseDTO.builder()
                .accessToken((String) result.get("accessToken"))
                .refreshToken((String) result.get("refreshToken"))
                .build();
        return ApiResponse.onSuccess("소셜 계정 연결에 성공했습니다.", dto);
    }

    @Operation(summary = "회원가입 이메일 전송")
    @PostMapping("/signup/email/send")
    public ApiResponse<Void> sendSignupEmail(
            HttpServletRequest request,
            @RequestBody @Valid EmailSendRequestDTO req)
    {
        limitEmailSend(request, "signup", req.getEmail());
        authService.sendSignupEmail(req.getEmail());

        return ApiResponse.onSuccess("회원가입 이메일 전송에 성공했습니다.", null);
    }

    @Operation(summary = "회원가입 이메일 인증")
    @PostMapping("/signup/email/verify")
    public ApiResponse<EmailVerifyResponseDTO> verifySignupEmail(
            HttpServletRequest request,
            @RequestBody @Valid EmailVerifyRequestDTO req)
    {
        limitEmailVerify(request);
        return ApiResponse.onSuccess("회원가입 이메일 인증에 성공했습니다.", authService.verifySignupEmail(req));
    }

    @Operation(summary = "대학 이메일 전송")
    @PostMapping("/email/send")
    public ApiResponse<Void> sendEmail(
            HttpServletRequest request,
            Authentication auth,
            @RequestBody @Valid EmailSendRequestDTO req)
    {
        var userId = ((CustomUserDetails) auth.getPrincipal()).getId();
        limitEmailSend(request, "university", req.getEmail());
        authService.sendEmail(userId, req.getEmail());

        return ApiResponse.onSuccess("이메일 전송에 성공했습니다.", null);
    }

    @Operation(summary = "대학 이메일 인증")
    @PostMapping("/email/verify")
    public ApiResponse<EmailVerifyResponseDTO> verifyEmail(
            HttpServletRequest request,
            Authentication auth,
            @RequestBody @Valid UnivEmailVerifyRequestDTO req)
    {
        var userId = ((CustomUserDetails) auth.getPrincipal()).getId();
        limitEmailVerify(request);
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
    public ApiResponse<LoginResponseDTO> login(
            HttpServletRequest request,
            @RequestBody @Valid LoginRequestDTO req)
    {
        String rateKey = clientIp(request) + '|' + req.getEmail().trim().toLowerCase();
        Duration window = Duration.ofMinutes(15);
        rateLimitService.assertAllowed("login", rateKey, 5, window);
        try {
            LoginResponseDTO response = authService.login(req);
            rateLimitService.reset("login", rateKey);
            return ApiResponse.onSuccess("로그인에 성공했습니다.", response);
        } catch (GeneralException exception) {
            if (exception.getErrorCode() == ErrorCode.AUTHENTICATION_FAILED) {
                rateLimitService.record("login", rateKey, window);
            }
            throw exception;
        }
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication auth)
    {
        var userId = ((CustomUserDetails) auth.getPrincipal()).getId();
        authService.logout(userId);

        return ApiResponse.onSuccess("로그아웃에 성공했습니다.", null);
    }


    @Operation(summary = "탈퇴 이메일 전송")
    @PostMapping("/withdraw")
    public ApiResponse<EmailVerifyResponseDTO> withdrawEmail(
            HttpServletRequest request,
            Authentication auth)
    {
        var userId = ((CustomUserDetails) auth.getPrincipal()).getId();
        limitEmailSend(request, "withdraw", userId.toString());
        authService.sendWithDrawEmail(userId);
        return ApiResponse.onSuccess("탈퇴 이메일 전송에 성공했습니다.", null);
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/withdraw")
    public ApiResponse<EmailVerifyResponseDTO> withdrawEmail(
            HttpServletRequest request,
            Authentication auth,
            @RequestBody @Valid WithDrawEmailVerifyRequestDTO req)
    {
        var userId = ((CustomUserDetails) auth.getPrincipal()).getId();
        limitEmailVerify(request);
        authService.withdraw(userId, req);
        return ApiResponse.onSuccess("회원 탈퇴에 성공했습니다", null);
    }

    @Operation(summary = "토큰 갱신")
    @PostMapping("/refreshToken")
    public ApiResponse<RefreshTokenResponseDTO> refreshToken(@RequestBody @Valid RefreshTokenRequestDTO req)
    {
        return ApiResponse.onSuccess("토큰 갱신에 성공했습니다.", authService.refreshToken(req.getRefreshToken()));
    }

    private void limitEmailSend(HttpServletRequest request, String purpose, String addressOrUser) {
        rateLimitService.consume(
                "email-send-ip", clientIp(request), 10, Duration.ofHours(1));
        rateLimitService.consume(
                "email-send-address", purpose + '|' + addressOrUser.trim().toLowerCase(),
                1, Duration.ofSeconds(60));
    }

    private void limitEmailVerify(HttpServletRequest request) {
        rateLimitService.consume(
                "email-verify-ip", clientIp(request), 30, Duration.ofMinutes(15));
    }

    private String clientIp(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }
}
