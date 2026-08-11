package com.hicct3.projectfinder.global.config;

import com.hicct3.projectfinder.websocket.ProjectChatHandshakeInterceptor;
import com.hicct3.projectfinder.websocket.ProjectChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ProjectChatWebSocketHandler projectChatWebSocketHandler;
    private final ProjectChatHandshakeInterceptor projectChatHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(projectChatWebSocketHandler, "/ws/projects/{projectId}")
                .addInterceptors(projectChatHandshakeInterceptor)
                .setAllowedOriginPatterns(
                        "https://zzoin.me",
                        "capacitor://localhost",
                        "http://localhost",
                        "https://localhost",
                        "http://localhost:5173");
    }
}
