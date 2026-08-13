package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.RefreshToken;
import com.hicct3.projectfinder.entity.User;
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
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
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
        assertEquals("access-token", result.get("accessToken"));
        assertEquals("refresh-token", result.get("refreshToken"));
    }
}
