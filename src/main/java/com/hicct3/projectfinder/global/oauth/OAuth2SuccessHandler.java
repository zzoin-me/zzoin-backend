package com.hicct3.projectfinder.global.oauth;

import com.hicct3.projectfinder.service.OAuthAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuthAuthService oAuthAuthService;

    @Value("${app.oauth2.front-redirect-base:http://localhost:5173}")
    private String frontRedirectBase;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        OAuth2Attributes attrs = OAuth2Attributes.builder()
                .provider((String) oAuth2User.getAttribute("_provider"))
                .providerId((String) oAuth2User.getAttribute("_providerId"))
                .email((String) oAuth2User.getAttribute("_email"))
                .emailVerified((Boolean) oAuth2User.getAttribute("_emailVerified"))
                .name((String) oAuth2User.getAttribute("_name"))
                .profileImageUrl((String) oAuth2User.getAttribute("_profileImageUrl"))
                .build();

        Map<String, Object> result = oAuthAuthService.processSocialLogin(attrs);
        OAuthAuthService.SocialLoginResult resultType = (OAuthAuthService.SocialLoginResult) result.get("result");

        if (resultType == OAuthAuthService.SocialLoginResult.RECOVERY) {
            ResponseCookie recoveryCookie = ResponseCookie.from(
                            "zzoin_recovery",
                            (String) result.get("recoveryToken")
                    )
                    .httpOnly(true)
                    .secure(frontRedirectBase.startsWith("https://"))
                    .sameSite("Lax")
                    .path("/api/auth/recover")
                    .maxAge(1800)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, recoveryCookie.toString());
        }

        String redirectUrl = switch (resultType) {
            case LOGIN -> buildTokenUrl("/auth/callback", result, false);
            case SIGNUP -> UriComponentsBuilder.fromUriString(frontRedirectBase + "/social-signup")
                    .queryParam("signupToken", result.get("signupToken"))
                    .queryParam("provider", result.get("provider"))
                    .queryParam("email", result.get("email"))
                    .queryParam("suggestedNickname", result.get("suggestedNickname"))
                    .build().encode().toUriString();
            case NEED_LINK -> UriComponentsBuilder.fromUriString(frontRedirectBase + "/link-account")
                    .queryParam("tempToken", result.get("tempToken"))
                    .queryParam("provider", result.get("provider"))
                    .queryParam("providerId", result.get("providerId"))
                    .build().toUriString();
            case CONFLICT_PROVIDER -> UriComponentsBuilder.fromUriString(frontRedirectBase + "/login")
                    .queryParam("error", "social_conflict")
                    .queryParam("existingProvider", result.get("existingProvider"))
                    .build().toUriString();
            case EMAIL_REQUIRED -> UriComponentsBuilder.fromUriString(frontRedirectBase + "/login")
                    .queryParam("error", "social_email_required")
                    .queryParam("provider", result.get("provider"))
                    .build().toUriString();
            case RECOVERY_PROVIDER_MISMATCH -> UriComponentsBuilder.fromUriString(frontRedirectBase + "/login")
                    .queryParam("error", "recovery_provider_mismatch")
                    .queryParam("existingProvider", result.get("existingProvider"))
                    .build().toUriString();
            case RECOVERY -> UriComponentsBuilder.fromUriString(frontRedirectBase + "/account-recovery")
                    .queryParam("recoverableUntil", result.get("recoverableUntil"))
                    .queryParam("provider", result.get("provider"))
                    .build().encode().toUriString();
        };

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String buildTokenUrl(String path, Map<String, Object> result, boolean isNew) {
        return UriComponentsBuilder.fromUriString(frontRedirectBase + path)
                .queryParam("accessToken", result.get("accessToken"))
                .queryParam("refreshToken", result.get("refreshToken"))
                .queryParam("isNew", isNew)
                .build().toUriString();
    }
}
