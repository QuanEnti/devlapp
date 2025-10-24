package com.devcollab.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    private final VertexAiGeminiChatModel geminiChatModel;

    // Spring Boot automatically creates this bean if your pom.xml and API key are correct
    public ChatConfig(VertexAiGeminiChatModel geminiChatModel) {
        this.geminiChatModel = geminiChatModel;
    }

    @Bean
    public ChatClient chatClient() {
        // This tells Spring how to create ChatClient
        return ChatClient.create(geminiChatModel);
    }
}
