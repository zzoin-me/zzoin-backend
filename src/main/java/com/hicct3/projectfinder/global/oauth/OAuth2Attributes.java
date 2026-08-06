package com.hicct3.projectfinder.global.oauth;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class OAuth2Attributes {
    private String provider;
    private String providerId;
    private String email;
    private Boolean emailVerified;
    private String name;
    private String profileImageUrl;

    public static OAuth2Attributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("kakao".equalsIgnoreCase(registrationId)) {
            return ofKakao(attributes);
        }
        return ofGoogle(attributes);
    }

    private static OAuth2Attributes ofGoogle(Map<String, Object> attributes) {
        Object verifiedObj = attributes.get("email_verified");
        Boolean emailVerified = verifiedObj instanceof Boolean ? (Boolean) verifiedObj : "true".equals(String.valueOf(verifiedObj));

        return OAuth2Attributes.builder()
                .provider("google")
                .providerId(String.valueOf(attributes.get("sub")))
                .email(String.valueOf(attributes.get("email")))
                .emailVerified(emailVerified)
                .name(String.valueOf(attributes.getOrDefault("name", "")))
                .profileImageUrl(String.valueOf(attributes.getOrDefault("picture", "")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private static OAuth2Attributes ofKakao(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;

        String email = kakaoAccount != null ? String.valueOf(kakaoAccount.getOrDefault("email", "")) : "";
        Object isEmailVerifiedObj = kakaoAccount != null ? kakaoAccount.get("is_email_verified") : null;
        Boolean emailVerified = isEmailVerifiedObj instanceof Boolean ? (Boolean) isEmailVerifiedObj : "true".equals(String.valueOf(isEmailVerifiedObj));

        String nickname = profile != null ? String.valueOf(profile.getOrDefault("nickname", "")) : "";
        String profileImageUrl = profile != null ? String.valueOf(profile.getOrDefault("profile_image_url", "")) : "";

        return OAuth2Attributes.builder()
                .provider("kakao")
                .providerId(String.valueOf(attributes.get("id")))
                .email(email)
                .emailVerified(emailVerified)
                .name(nickname)
                .profileImageUrl(profileImageUrl)
                .build();
    }
}
