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
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // ✅ Simple message broker for broadcasting
        // "/topic" → group messages (e.g., project chat)
        // "/queue" → private messages (e.g., notifications)
        registry.enableSimpleBroker("/topic", "/queue");

        // ✅ Prefix for messages sent from client to server
        // Example: client sends to /app/chat/{projectId}
        registry.setApplicationDestinationPrefixes("/app");

        // ✅ Allow server to send messages directly to a specific user
        // Example: server sends to /user/{email}/queue/notifications
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ✅ WebSocket handshake endpoint
        // SockJS fallback ensures support on all browsers
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // allow all origins (you can restrict later)
                .withSockJS();
    }
}
