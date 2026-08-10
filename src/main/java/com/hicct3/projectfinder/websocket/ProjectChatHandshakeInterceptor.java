package com.hicct3.projectfinder.websocket;

import com.hicct3.projectfinder.global.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ProjectChatHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID = "chatUserId";
    public static final String PROJECT_ID = "chatProjectId";
    private static final Pattern PROJECT_PATH = Pattern.compile("/ws/projects/(\\d+)$");

    private final JwtProvider jwtProvider;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        Matcher matcher = PROJECT_PATH.matcher(request.getURI().getPath());
        if (token == null || !jwtProvider.validateToken(token) || !matcher.find()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            attributes.put(USER_ID, jwtProvider.verifyAccessTokenAndGetUserId(token));
            attributes.put(PROJECT_ID, Long.valueOf(matcher.group(1)));
            return true;
        } catch (RuntimeException exception) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
    }
}
