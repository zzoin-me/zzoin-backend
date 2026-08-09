package com.hicct3.projectfinder.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.global.JwtAuthenticationFilter;
import com.hicct3.projectfinder.global.oauth.CustomOAuth2UserService;
import com.hicct3.projectfinder.global.oauth.OAuth2FailureHandler;
import com.hicct3.projectfinder.global.oauth.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2SuccessHandler oAuth2SuccessHandler,
            OAuth2FailureHandler oAuth2FailureHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
    }


    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST API와 JWT 기반 인증에서는 일반적으로 CSRF를 비활성화합니다.
                .csrf(csrf -> csrf.disable())

                // CORS 설정
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOriginPatterns(java.util.List.of(
                            "*",
                            "capacitor://localhost",
                            "http://localhost",
                            "http://localhost:5173",
                            "https://zzoin.vercel.app"
                    ));
                    config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(java.util.List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))

                // JWT 기반 인증에서는 서버 세션을 사용하지 않습니다.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // OAuth2 소셜 로그인
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )

                // 인증/인가 실패 응답 설정
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");

                            objectMapper.writeValue(
                                    response.getWriter(),
                                    ApiResponse.onError("인증이 필요합니다.")
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");

                            objectMapper.writeValue(
                                    response.getWriter(),
                                    ApiResponse.onError("접근 권한이 없습니다.")
                            );
                        })
                )

                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/signup").permitAll()
                        .requestMatchers("/api/auth/signup/email/send").permitAll()
                        .requestMatchers("/api/auth/signup/email/verify").permitAll()
                        .requestMatchers("/api/auth/refreshToken").permitAll()
                        .requestMatchers("/api/auth/link-account").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/api/univs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/project-feeds/popular").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/project-feeds/recommend").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/projects", "/api/projects/{projectId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/job-roles/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/stacks").hasRole("ADMIN")
                        .requestMatchers("/api/projects/**").hasRole("VERIFIED")
                        .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/{postId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts/{postId}/comments").permitAll()
                        .requestMatchers("/api/posts/**").hasRole("VERIFIED")
                        .requestMatchers("/api/comments/**").hasRole("VERIFIED")
                        .requestMatchers(HttpMethod.GET, "/api/notifications/stream").permitAll()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .anyRequest().authenticated()
                )

                // UsernamePasswordAuthenticationFilter 전에 JWT 인증 필터를 실행합니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
