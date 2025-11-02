package com.devcollab.controller.rest;

import com.devcollab.dto.request.MessageRequestDTO;
import com.devcollab.dto.response.MessageResponseDTO;
import com.devcollab.domain.Message;
import com.devcollab.service.feature.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class UserMessageRestController {

    private final MessageService messageService;

    /**
     * ✅ Hàm tái sử dụng để lấy email từ Authentication (Google OAuth2 / Local login)
     */
    private String getEmailFromAuthentication(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauthToken) {
            return oauthToken.getPrincipal().getAttribute("email");
        }
        return auth.getName(); // Đối với login thường (username/password)
    }

    // ✉️ Gửi tin nhắn
    @PostMapping("/send")
    public MessageResponseDTO sendMessage(@RequestBody MessageRequestDTO request, Authentication auth) {
        System.out.println("✅ API /api/messages/send called");

        // ⛔ Không dùng auth.getName(), phải lấy đúng email
        String senderEmail = getEmailFromAuthentication(auth);
        System.out.println("📌 Sender email = " + senderEmail);

        // Gửi tin nhắn
        Message msg = messageService.sendMessage(senderEmail, request);

        // Trả về DTO cho frontend
        return new MessageResponseDTO(
                msg.getMessageId(),
                msg.getSender().getName(),
                msg.getSender().getEmail(),
                msg.getSender().getAvatarUrl(),
                msg.getContent(),
                msg.getCreatedAt()
        );
    }

    // 📩 Lấy danh sách tin nhắn theo projectId
    @GetMapping("/{projectId}")
    public List<MessageResponseDTO> getMessagesByProject(@PathVariable Long projectId) {
        return messageService.getMessagesByProjectId(projectId);
    }
}
