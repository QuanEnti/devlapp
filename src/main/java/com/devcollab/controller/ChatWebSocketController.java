package com.devcollab.controller;

import com.devcollab.dto.request.MessageRequestDTO;
import com.devcollab.dto.response.MessageResponseDTO;
import com.devcollab.domain.Message;
import com.devcollab.service.feature.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    @MessageMapping("/chat/{projectId}") // client sends to /app/chat/{projectId}
    public void handleChat(@DestinationVariable Long projectId, MessageRequestDTO dto) {
        dto.setProjectId(projectId);
        Message msg = messageService.sendMessage(dto.getSenderEmail(), dto);

        // Build response DTO for broadcast
        MessageResponseDTO response = new MessageResponseDTO(
                msg.getMessageId(),
                msg.getSender().getName(),
                msg.getSender().getEmail(),
                msg.getSender().getAvatarUrl(),
                msg.getContent(),
                msg.getCreatedAt()
        );

        messagingTemplate.convertAndSend("/topic/project." + projectId, response);
    }
}
