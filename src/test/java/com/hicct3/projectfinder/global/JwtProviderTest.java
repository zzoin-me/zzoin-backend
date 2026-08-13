package com.hicct3.projectfinder.global;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtProviderTest {
    private final JwtProvider jwtProvider = new JwtProvider(
            "test-secret-key-with-at-least-32-bytes"
    );

    @Test
    void createsAndVerifiesSocialSignupToken() {
        String token = jwtProvider.createSocialSignupToken(
                "new@example.com",
                "kakao",
                "provider-id",
                "카카오사용자",
                "https://example.com/profile.png",
                true
        );

        JwtProvider.SocialSignupClaims claims = jwtProvider.verifySocialSignupToken(token);

        assertEquals("new@example.com", claims.email());
        assertEquals("kakao", claims.provider());
        assertEquals("provider-id", claims.providerId());
        assertEquals("카카오사용자", claims.suggestedNickname());
        assertEquals("https://example.com/profile.png", claims.profileImageUrl());
        assertTrue(claims.emailVerified());
    }
}
