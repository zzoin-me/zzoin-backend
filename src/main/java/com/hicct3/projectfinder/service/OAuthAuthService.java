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
            return buildTokenResponse(user, SocialLoginResult.LOGIN);
        }

        if (attrs.getEmail() != null && !attrs.getEmail().isBlank()) {
            String lowerEmail = attrs.getEmail().trim().toLowerCase();
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
                    return buildTokenResponse(existing, SocialLoginResult.LOGIN);
                }

                if (Boolean.TRUE.equals(attrs.getEmailVerified())) {
                    String linkToken = jwtProvider.createSignupToken(lowerEmail);
                    return Map.of(
                            "result", SocialLoginResult.NEED_LINK,
                            "tempToken", linkToken,
                            "provider", attrs.getProvider(),
                            "providerId", attrs.getProviderId()
                    );
                }

                return Map.of(
                        "result", SocialLoginResult.NEED_LINK,
                        "tempToken", jwtProvider.createSignupToken(lowerEmail),
                        "provider", attrs.getProvider(),
                        "providerId", attrs.getProviderId()
                );
            }
        }

        User newUser = createSocialUser(attrs);
        return buildTokenResponse(newUser, SocialLoginResult.SIGNUP);
    }

    @Transactional
    public Map<String, Object> linkAccount(String tempToken, String password, String provider, String providerId) {
        String email = jwtProvider.verifySignupTokenAndGetEmail(tempToken).trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new GeneralException(ErrorCode.USER_WITHDRAWN);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new GeneralException(ErrorCode.AUTHENTICATION_FAILED);
        }

        user.setProvider(provider);
        user.setProviderId(providerId);

        return buildTokenResponse(user, SocialLoginResult.LOGIN);
    }

    private User createSocialUser(OAuth2Attributes attrs) {
        String lowerEmail = attrs.getEmail() != null ? attrs.getEmail().trim().toLowerCase() : "";
        String nickname = generateUniqueNickname(attrs);

        User user = User.builder()
                .nickName(nickname)
                .email(lowerEmail.isBlank() ? (attrs.getProvider() + "_" + attrs.getProviderId() + "@social.local") : lowerEmail)
                .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                .verified(Boolean.TRUE.equals(attrs.getEmailVerified()))
                .admin(false)
                .provider(attrs.getProvider())
                .providerId(attrs.getProviderId())
                .build();

        return userRepository.save(user);
    }

    private String generateUniqueNickname(OAuth2Attributes attrs) {
        String base = attrs.getName();
        if (base == null || base.isBlank()) {
            base = attrs.getProvider();
        }
        String candidate = base;
        int suffix = 0;
        while (userRepository.existsByNickName(candidate)) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
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
