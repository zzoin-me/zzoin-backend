package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.RefreshToken;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.global.JwtProvider;
import com.hicct3.projectfinder.global.oauth.OAuth2Attributes;
import com.hicct3.projectfinder.repository.RefreshTokenRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthAuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AccountLifecycleService accountLifecycleService;

    @InjectMocks
    private OAuthAuthService oAuthAuthService;

    @Test
    void newSocialAccountReturnsPendingSignupWithoutCreatingUser() {
        OAuth2Attributes attributes = OAuth2Attributes.builder()
                .provider("kakao")
                .providerId("provider-id")
                .email("new@example.com")
                .emailVerified(true)
                .name("카카오사용자")
                .profileImageUrl("https://example.com/profile.png")
                .build();
        when(userRepository.findByProviderAndProviderId("kakao", "provider-id"))
                .thenReturn(Optional.empty());
        when(userRepository.findByAnyEmail("new@example.com")).thenReturn(Optional.empty());
        when(jwtProvider.createSocialSignupToken(
                "new@example.com",
                "kakao",
                "provider-id",
                "카카오사용자",
                "https://example.com/profile.png",
                true
        )).thenReturn("pending-signup-token");

        Map<String, Object> result = oAuthAuthService.processSocialLogin(attributes);

        assertEquals(OAuthAuthService.SocialLoginResult.SIGNUP, result.get("result"));
        assertEquals("pending-signup-token", result.get("signupToken"));
        assertEquals("카카오사용자", result.get("suggestedNickname"));
        verify(userRepository, never()).save(any(User.class));
        verify(jwtProvider, never()).createAccessToken(any());
        verify(jwtProvider, never()).createRefreshToken(any());
    }

    @Test
    void completingSocialSignupCreatesUserWithChosenNicknameAndReturnsTokens() {
        JwtProvider.SocialSignupClaims claims = new JwtProvider.SocialSignupClaims(
                "new@example.com",
                "kakao",
                "provider-id",
                "카카오사용자",
                "https://example.com/profile.png",
                true
        );
        when(jwtProvider.verifySocialSignupToken("pending-signup-token")).thenReturn(claims);
        when(userRepository.findByProviderAndProviderId("kakao", "provider-id"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByNickName("새닉네임")).thenReturn(false);
        when(userRepository.findByAnyEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(42L);
            return user;
        });
        when(jwtProvider.createAccessToken(42L)).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(42L)).thenReturn("refresh-token");
        when(refreshTokenRepository.findByUserId(42L)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = oAuthAuthService.completeSocialSignup(
                "pending-signup-token",
                "새닉네임"
        );

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("새닉네임", savedUser.getNickName());
        assertEquals("new@example.com", savedUser.getEmail());
        assertEquals("kakao", savedUser.getProvider());
        assertEquals("provider-id", savedUser.getProviderId());
        assertEquals("https://example.com/profile.png", savedUser.getProfileUrl());
        assertFalse(savedUser.isDeleted());
        assertFalse(savedUser.getVerified());
        assertNull(savedUser.getVerifiedEmail());
        assertNull(savedUser.getSchoolDomain());
        assertEquals("access-token", result.get("accessToken"));
        assertEquals("refresh-token", result.get("refreshToken"));
    }

    @Test
    void newSocialAccountWithoutEmailCannotStartSignup() {
        OAuth2Attributes attributes = OAuth2Attributes.builder()
                .provider("kakao")
                .providerId("provider-id")
                .email("")
                .name("카카오사용자")
                .build();
        when(userRepository.findByProviderAndProviderId("kakao", "provider-id"))
                .thenReturn(Optional.empty());

        Map<String, Object> result = oAuthAuthService.processSocialLogin(attributes);

        assertEquals(OAuthAuthService.SocialLoginResult.EMAIL_REQUIRED, result.get("result"));
        assertEquals("kakao", result.get("provider"));
        verify(jwtProvider, never()).createSocialSignupToken(
                any(), any(), any(), any(), any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void socialSignupTokenWithoutEmailIsRejected() {
        JwtProvider.SocialSignupClaims claims = new JwtProvider.SocialSignupClaims(
                "",
                "kakao",
                "provider-id",
                "카카오사용자",
                "",
                false
        );
        when(jwtProvider.verifySocialSignupToken("pending-signup-token")).thenReturn(claims);
        when(userRepository.findByProviderAndProviderId("kakao", "provider-id"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByNickName("새닉네임")).thenReturn(false);

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> oAuthAuthService.completeSocialSignup("pending-signup-token", "새닉네임")
        );

        assertEquals(ErrorCode.SOCIAL_EMAIL_REQUIRED, exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void differentSocialProviderWithSameEmailReturnsProviderConflict() {
        OAuth2Attributes attributes = OAuth2Attributes.builder()
                .provider("google")
                .providerId("google-id")
                .email("member@example.com")
                .emailVerified(true)
                .name("구글사용자")
                .build();
        User kakaoUser = User.builder()
                .userId(1L)
                .nickName("회원")
                .email("member@example.com")
                .password("encoded")
                .provider("kakao")
                .providerId("kakao-id")
                .localLoginEnabled(false)
                .verified(true)
                .admin(false)
                .build();
        when(userRepository.findByProviderAndProviderId("google", "google-id"))
                .thenReturn(Optional.empty());
        when(userRepository.findByAnyEmail("member@example.com"))
                .thenReturn(Optional.of(kakaoUser));

        Map<String, Object> result = oAuthAuthService.processSocialLogin(attributes);

        assertEquals(OAuthAuthService.SocialLoginResult.CONFLICT_PROVIDER, result.get("result"));
        assertEquals("kakao", result.get("existingProvider"));
        verify(jwtProvider, never()).createSocialSignupToken(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void recoverableWithdrawnSocialAccountReturnsRecoveryResult() {
        OAuth2Attributes attributes = OAuth2Attributes.builder()
                .provider("kakao")
                .providerId("kakao-id")
                .email("member@example.com")
                .build();
        User withdrawnUser = User.builder()
                .userId(7L)
                .nickName("회원")
                .email("member@example.com")
                .password("encoded")
                .provider("kakao")
                .providerId("kakao-id")
                .localLoginEnabled(false)
                .verified(true)
                .admin(false)
                .deletedAt(java.time.LocalDateTime.of(2026, 8, 1, 12, 0))
                .build();
        when(userRepository.findByProviderAndProviderId("kakao", "kakao-id"))
                .thenReturn(Optional.of(withdrawnUser));
        when(accountLifecycleService.finalizeIfExpired(withdrawnUser)).thenReturn(false);
        when(jwtProvider.createAccountRecoveryToken(7L)).thenReturn("recovery-token");

        Map<String, Object> result = oAuthAuthService.processSocialLogin(attributes);

        assertEquals(OAuthAuthService.SocialLoginResult.RECOVERY, result.get("result"));
        assertEquals("recovery-token", result.get("recoveryToken"));
        assertEquals("kakao", result.get("provider"));
        verify(jwtProvider, never()).createAccessToken(any());
    }

    @Test
    void withdrawnAccountCannotBeRecoveredWithDifferentSocialProvider() {
        OAuth2Attributes attributes = OAuth2Attributes.builder()
                .provider("google")
                .providerId("google-id")
                .email("member@example.com")
                .emailVerified(true)
                .build();
        User withdrawnKakaoUser = User.builder()
                .userId(7L)
                .nickName("회원")
                .email("member@example.com")
                .password("encoded")
                .provider("kakao")
                .providerId("kakao-id")
                .localLoginEnabled(false)
                .verified(false)
                .admin(false)
                .deletedAt(java.time.LocalDateTime.of(2026, 8, 1, 12, 0))
                .build();
        when(userRepository.findByProviderAndProviderId("google", "google-id"))
                .thenReturn(Optional.empty());
        when(userRepository.findByAnyEmail("member@example.com"))
                .thenReturn(Optional.of(withdrawnKakaoUser));
        when(accountLifecycleService.finalizeIfExpired(withdrawnKakaoUser)).thenReturn(false);

        Map<String, Object> result = oAuthAuthService.processSocialLogin(attributes);

        assertEquals(
                OAuthAuthService.SocialLoginResult.RECOVERY_PROVIDER_MISMATCH,
                result.get("result")
        );
        assertEquals("kakao", result.get("existingProvider"));
        verify(jwtProvider, never()).createAccountRecoveryToken(any());
        verify(jwtProvider, never()).createAccessToken(any());
    }

    @Test
    void socialOnlyAccountCannotUnlinkItsOnlyLoginMethod() {
        User socialUser = User.builder()
                .userId(9L)
                .nickName("회원")
                .email("member@example.com")
                .password("encoded")
                .provider("google")
                .providerId("google-id")
                .localLoginEnabled(false)
                .verified(true)
                .admin(false)
                .build();
        when(userRepository.findById(9L)).thenReturn(Optional.of(socialUser));

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> oAuthAuthService.unlinkSocial(9L, "password")
        );

        assertEquals(ErrorCode.SOCIAL_UNLINK_NOT_ALLOWED, exception.getErrorCode());
    }
}
