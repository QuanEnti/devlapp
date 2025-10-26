package com.devcollab.controller.rest;

import com.devcollab.dto.request.MessageRequestDTO;
import com.devcollab.dto.response.MessageResponseDTO;
import com.devcollab.domain.Message;
import com.devcollab.service.feature.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class UserMessageRestController {

    private final MessageService messageService;

    // 📩 Lấy danh sách tin nhắn theo projectId
    @GetMapping("/{projectId}")
    public List<MessageResponseDTO> getMessagesByProject(@PathVariable Long projectId) {
        return messageService.getMessagesByProjectId(projectId);
    }

    // ✉️ Gửi tin nhắn mới
    @PostMapping("/send")
    public MessageResponseDTO sendMessage(@RequestBody MessageRequestDTO request, Authentication auth) {
        String senderUsername = auth.getName();
        Message msg = messageService.sendMessage(senderUsername, request);

        return new MessageResponseDTO(
                msg.getMessageId(),
                msg.getSender().getName(),
                msg.getSender().getEmail(),
                msg.getContent(),
                msg.getCreatedAt());
    }
}
