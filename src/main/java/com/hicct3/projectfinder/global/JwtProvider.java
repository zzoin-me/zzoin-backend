package com.hicct3.projectfinder.global;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {
    private final SecretKey key;
    private final long LOGIN_TOKEN_VALIDITY = 1000L * 60 * 60;
    private final long REFRESH_TOKEN_VALIDITY = 1000L * 60 * 60 * 24 * 14;
    private final long SIGNUP_TOKEN_VALIDITY = 1000L * 60 * 10;
    private final long SOCIAL_SIGNUP_TOKEN_VALIDITY = 1000L * 60 * 30;
    private final long ACCOUNT_RECOVERY_TOKEN_VALIDITY = 1000L * 60 * 30;


    public JwtProvider(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long id) {
        return buildToken(id.toString(), "ACCESS", LOGIN_TOKEN_VALIDITY);
    }

    public String createRefreshToken(Long id) {
        return buildToken(id.toString(), "REFRESH-TOKEN", REFRESH_TOKEN_VALIDITY);
    }

    public String createSignupToken(String email) {
        return buildToken(email, "SIGNUP", SIGNUP_TOKEN_VALIDITY);
    }

    public String createAccountRecoveryToken(Long userId) {
        return buildToken(userId.toString(), "ACCOUNT_RECOVERY", ACCOUNT_RECOVERY_TOKEN_VALIDITY);
    }

    public String createSocialLinkToken(String email, String provider, String providerId, String profileImageUrl) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + SIGNUP_TOKEN_VALIDITY);

        return Jwts.builder()
                .subject(email)
                .claim("token_type", "SOCIAL_LINK")
                .claim("provider", provider)
                .claim("provider_id", providerId)
                .claim("profile_image_url", profileImageUrl == null ? "" : profileImageUrl)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public String createSocialSignupToken(
            String email,
            String provider,
            String providerId,
            String suggestedNickname,
            String profileImageUrl,
            Boolean emailVerified
    ) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + SOCIAL_SIGNUP_TOKEN_VALIDITY);

        return Jwts.builder()
                .subject(providerId)
                .claim("token_type", "SOCIAL_SIGNUP")
                .claim("email", email == null ? "" : email)
                .claim("provider", provider)
                .claim("provider_id", providerId)
                .claim("suggested_nickname", suggestedNickname == null ? "" : suggestedNickname)
                .claim("profile_image_url", profileImageUrl == null ? "" : profileImageUrl)
                .claim("email_verified", Boolean.TRUE.equals(emailVerified))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }


    public Long verifyAccessTokenAndGetUserId(String token) {
        Claims claims = getClaims(token);

        if (!"ACCESS".equals(claims.get("token_type"))) {
            throw new GeneralException("API 접근용 Access Token이 아닙니다.");
        }

        return Long.valueOf(claims.getSubject());
    }

    public Long verifyRefreshTokenAndGetUserId(String token) {
        Claims claims = getClaims(token);

        if (!"REFRESH-TOKEN".equals(claims.get("token_type"))) {
            throw new GeneralException("리프레시 토큰이 아닙니다.");
        }

        return Long.valueOf(claims.getSubject());
    }

    public String verifySignupTokenAndGetEmail(String token) {
        Claims claims = getClaims(token);

        if (!"SIGNUP".equals(claims.get("token_type"))) {
            throw new GeneralException("회원가입용 토큰이 아닙니다.");
        }

        return claims.getSubject();
    }

    public SocialLinkClaims verifySocialLinkToken(String token) {
        Claims claims = getClaims(token);

        if (!"SOCIAL_LINK".equals(claims.get("token_type"))) {
            throw new GeneralException(ErrorCode.INVALID_TOKEN);
        }

        return new SocialLinkClaims(
                claims.getSubject(),
                claims.get("provider", String.class),
                claims.get("provider_id", String.class),
                claims.get("profile_image_url", String.class)
        );
    }

    public SocialSignupClaims verifySocialSignupToken(String token) {
        Claims claims = getClaims(token);

        if (!"SOCIAL_SIGNUP".equals(claims.get("token_type"))) {
            throw new GeneralException(ErrorCode.INVALID_TOKEN);
        }

        return new SocialSignupClaims(
                claims.get("email", String.class),
                claims.get("provider", String.class),
                claims.get("provider_id", String.class),
                claims.get("suggested_nickname", String.class),
                claims.get("profile_image_url", String.class),
                claims.get("email_verified", Boolean.class)
        );
    }

    public Long verifyAccountRecoveryToken(String token) {
        Claims claims = getClaims(token);
        if (!"ACCOUNT_RECOVERY".equals(claims.get("token_type"))) {
            throw new GeneralException(ErrorCode.INVALID_TOKEN);
        }
        return Long.valueOf(claims.getSubject());
    }

    public record SocialLinkClaims(
            String email,
            String provider,
            String providerId,
            String profileImageUrl
    ) {
    }

    public record SocialSignupClaims(
            String email,
            String provider,
            String providerId,
            String suggestedNickname,
            String profileImageUrl,
            Boolean emailVerified
    ) {
    }


    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String buildToken(String subject, String type, long validityTime) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + validityTime);

        return Jwts.builder()
                .subject(subject)
                .claim("token_type", type) // 토큰의 용도 구분
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }



    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new GeneralException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new GeneralException(ErrorCode.INVALID_TOKEN);
        }
    }
}
