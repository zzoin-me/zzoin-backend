package com.hicct3.projectfinder.global.oauth;

import com.hicct3.projectfinder.service.OAuthAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

        String redirectUrl = switch (resultType) {
            case LOGIN -> buildTokenUrl("/auth/callback", result, false);
            case SIGNUP -> buildTokenUrl("/auth/callback", result, true);
            case NEED_LINK -> UriComponentsBuilder.fromUriString(frontRedirectBase + "/link-account")
                    .queryParam("tempToken", result.get("tempToken"))
                    .queryParam("provider", result.get("provider"))
                    .queryParam("providerId", result.get("providerId"))
                    .build().toUriString();
            case CONFLICT_PROVIDER -> UriComponentsBuilder.fromUriString(frontRedirectBase + "/login")
                    .queryParam("error", "social_conflict")
                    .queryParam("existingProvider", result.get("existingProvider"))
                    .build().toUriString();
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
