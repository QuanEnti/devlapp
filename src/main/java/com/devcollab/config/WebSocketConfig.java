package com.devcollab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // ✅ Client sẽ subscribe các topic này để nhận thông báo
        config.enableSimpleBroker("/topic", "/queue");

        // ✅ Prefix cho các endpoint mà client có thể gửi tin (nếu có)
        config.setApplicationDestinationPrefixes("/app");

        // ✅ Định tuyến riêng cho từng user (sẽ gửi đến
        // /user/{email}/queue/notifications)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ✅ Endpoint để frontend kết nối WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // fallback nếu browser không hỗ trợ WS
    }
}
