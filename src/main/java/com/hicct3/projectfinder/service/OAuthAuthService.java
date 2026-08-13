package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.RefreshToken;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.global.JwtProvider;
import com.hicct3.projectfinder.global.oauth.OAuth2Attributes;
import com.hicct3.projectfinder.repository.RefreshTokenRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuthAuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    public enum SocialLoginResult {
        LOGIN,
        SIGNUP,
        NEED_LINK,
        CONFLICT_PROVIDER
    }

    @Transactional
    public Map<String, Object> processSocialLogin(OAuth2Attributes attrs) {
        Optional<User> byProvider = userRepository.findByProviderAndProviderId(attrs.getProvider(), attrs.getProviderId());

        if (byProvider.isPresent()) {
            User user = byProvider.get();
            if (user.isDeleted()) {
                throw new GeneralException(ErrorCode.USER_WITHDRAWN);
            }
            updateSocialProfile(user, attrs.getProfileImageUrl());
            return buildTokenResponse(user, SocialLoginResult.LOGIN);
        }

        String socialEmail = normalizeSocialValue(attrs.getEmail());
        if (!socialEmail.isBlank()) {
            String lowerEmail = socialEmail.toLowerCase();
            Optional<User> byEmail = userRepository.findByEmail(lowerEmail);

            if (byEmail.isPresent()) {
                User existing = byEmail.get();
                if (existing.isDeleted()) {
                    throw new GeneralException(ErrorCode.USER_WITHDRAWN);
                }

                if (existing.getProvider() != null && !"local".equals(existing.getProvider())) {
                    return Map.of(
                            "result", SocialLoginResult.CONFLICT_PROVIDER,
                            "existingProvider", existing.getProvider()
                    );
                }

                if (Boolean.TRUE.equals(attrs.getEmailVerified()) && Boolean.TRUE.equals(existing.getVerified())) {
                    existing.setProvider(attrs.getProvider());
                    existing.setProviderId(attrs.getProviderId());
                    updateSocialProfile(existing, attrs.getProfileImageUrl());
                    return buildTokenResponse(existing, SocialLoginResult.LOGIN);
                }

                if (Boolean.TRUE.equals(attrs.getEmailVerified())) {
                    String linkToken = jwtProvider.createSocialLinkToken(
                            lowerEmail,
                            attrs.getProvider(),
                            attrs.getProviderId(),
                            attrs.getProfileImageUrl()
                    );
                    return Map.of(
                            "result", SocialLoginResult.NEED_LINK,
                            "tempToken", linkToken,
                            "provider", attrs.getProvider(),
                            "providerId", attrs.getProviderId()
                    );
                }

                return Map.of(
                        "result", SocialLoginResult.NEED_LINK,
                        "tempToken", jwtProvider.createSocialLinkToken(
                                lowerEmail,
                                attrs.getProvider(),
                                attrs.getProviderId(),
                                attrs.getProfileImageUrl()
                        ),
                        "provider", attrs.getProvider(),
                        "providerId", attrs.getProviderId()
                );
            }
        }

        return buildSocialSignupResponse(attrs, socialEmail);
    }

    @Transactional
    public Map<String, Object> completeSocialSignup(String signupToken, String nickName) {
        JwtProvider.SocialSignupClaims claims = jwtProvider.verifySocialSignupToken(signupToken);

        Optional<User> byProvider = userRepository.findByProviderAndProviderId(
                claims.provider(),
                claims.providerId()
        );
        if (byProvider.isPresent()) {
            User existing = byProvider.get();
            if (existing.isDeleted()) {
                throw new GeneralException(ErrorCode.USER_WITHDRAWN);
            }
            return buildTokenResponse(existing, SocialLoginResult.LOGIN);
        }

        String normalizedNickname = nickName.trim();
        if (userRepository.existsByNickName(normalizedNickname)) {
            throw new GeneralException(ErrorCode.DUPLICATE_NICKNAME);
        }

        String email = resolveSocialEmail(claims.email(), claims.provider(), claims.providerId());
        if (userRepository.findByAnyEmail(email).isPresent()) {
            throw new GeneralException(ErrorCode.DUPLICATE_EMAIL);
        }

        User newUser = createSocialUser(claims, normalizedNickname, email);
        return buildTokenResponse(newUser, SocialLoginResult.SIGNUP);
    }

    @Transactional
    public Map<String, Object> linkAccount(String tempToken, String password) {
        JwtProvider.SocialLinkClaims linkClaims = jwtProvider.verifySocialLinkToken(tempToken);
        String email = linkClaims.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new GeneralException(ErrorCode.USER_WITHDRAWN);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new GeneralException(ErrorCode.AUTHENTICATION_FAILED);
        }

        user.setProvider(linkClaims.provider());
        user.setProviderId(linkClaims.providerId());
        updateSocialProfile(user, linkClaims.profileImageUrl());

        return buildTokenResponse(user, SocialLoginResult.LOGIN);
    }

    private Map<String, Object> buildSocialSignupResponse(OAuth2Attributes attrs, String socialEmail) {
        String suggestedNickname = normalizeSocialValue(attrs.getName());
        String signupToken = jwtProvider.createSocialSignupToken(
                socialEmail,
                attrs.getProvider(),
                attrs.getProviderId(),
                suggestedNickname,
                normalizeProfileImageUrl(attrs.getProfileImageUrl()),
                attrs.getEmailVerified()
        );

        return Map.of(
                "result", SocialLoginResult.SIGNUP,
                "signupToken", signupToken,
                "provider", attrs.getProvider(),
                "email", socialEmail,
                "suggestedNickname", suggestedNickname
        );
    }

    private User createSocialUser(
            JwtProvider.SocialSignupClaims claims,
            String nickname,
            String email
    ) {
        String socialProfileUrl = normalizeProfileImageUrl(claims.profileImageUrl());
        boolean hasProviderEmail = !normalizeSocialValue(claims.email()).isBlank();
        User user = User.builder()
                .nickName(nickname)
                .email(email)
                .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                .verified(hasProviderEmail && Boolean.TRUE.equals(claims.emailVerified()))
                .admin(false)
                .provider(claims.provider())
                .providerId(claims.providerId())
                .profileUrl(socialProfileUrl)
                .socialProfileUrl(socialProfileUrl)
                .nicknameChangedAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    private void updateSocialProfile(User user, String profileImageUrl) {
        String nextSocialProfileUrl = normalizeProfileImageUrl(profileImageUrl);
        String previousSocialProfileUrl = user.getSocialProfileUrl();
        boolean usingSocialProfile = user.getProfileUrl() == null
                || user.getProfileUrl().equals(previousSocialProfileUrl);
        user.setSocialProfileUrl(nextSocialProfileUrl);
        if (usingSocialProfile) {
            user.setProfileUrl(nextSocialProfileUrl);
        }
    }

    private String normalizeProfileImageUrl(String profileImageUrl) {
        String normalized = normalizeSocialValue(profileImageUrl);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeSocialValue(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return "null".equalsIgnoreCase(normalized) ? "" : normalized;
    }

    private String resolveSocialEmail(String email, String provider, String providerId) {
        String normalizedEmail = normalizeSocialValue(email).toLowerCase();
        return normalizedEmail.isBlank()
                ? provider + "_" + providerId + "@social.local"
                : normalizedEmail;
    }

    private Map<String, Object> buildTokenResponse(User user, SocialLoginResult result) {
        String accessToken = jwtProvider.createAccessToken(user.getUserId());
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId());

        refreshTokenRepository.findByUserId(user.getUserId())
                .ifPresentOrElse(
                        tokenEntity -> tokenEntity.update(refreshToken),
                        () -> refreshTokenRepository.save(RefreshToken.builder()
                                .userId(user.getUserId())
                                .token(refreshToken)
                                .build())
                );

        return Map.of(
                "result", result,
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );
    }
}
