package com.devcollab.service.impl.system;

import com.devcollab.domain.Notification;
import com.devcollab.domain.User;
import com.devcollab.dto.response.NotificationResponseDTO;
import com.devcollab.service.system.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationServiceImpl implements WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendToUser(User receiver, Notification notification, User sender) {
        if (receiver == null || notification == null) {
            log.warn("⚠️ [WebSocket] Receiver hoặc Notification null — bỏ qua gửi realtime.");
            return;
        }

        String email = receiver.getEmail();
        if (email == null || email.isBlank()) {
            log.warn("⚠️ [WebSocket] Email user null hoặc rỗng → không thể gửi.");
            return;
        }

        try {
            NotificationResponseDTO dto = NotificationResponseDTO.builder()
                    .id(notification.getNotificationId())
                    .type(notification.getType())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .status(notification.getStatus())
                    .createdAt(notification.getCreatedAt() != null ? notification.getCreatedAt() : LocalDateTime.now())
                    .referenceId(notification.getReferenceId())
                    .link(notification.getLink())
                    .icon(mapIcon(notification.getType()))
                    .senderName(sender != null && sender.getName() != null ? sender.getName() : "Hệ thống")
                    .senderAvatar(sender != null && sender.getAvatarUrl() != null
                            ? sender.getAvatarUrl()
                            : "https://cdn-icons-png.flaticon.com/512/149/149071.png")
                    .build();

            String destination = "/user/" + email + "/queue/notifications";
            messagingTemplate.convertAndSend(destination, dto);

            log.info("📡 [WebSocket] Sent → {} | Type={} | Title='{}' | From={}",
                    email, notification.getType(), notification.getTitle(),
                    sender != null ? sender.getEmail() : "System");

        } catch (Exception e) {
            log.error("❌ [WebSocket] Lỗi gửi notification: {}", e.getMessage(), e);
        }
    }

    private String mapIcon(String type) {
        if (type == null)
            return "🔔";
        return switch (type.toUpperCase()) {
            case "TASK_MEMBER_ADDED" -> "👥";
            case "TASK_MEMBER_REMOVED" -> "❌";
            case "TASK_COMMENTED" -> "💬";
            case "TASK_DUE_SOON" -> "⏰";
            case "TASK_ATTACHMENT_ADDED" -> "📎";
            case "TASK_ATTACHMENT_DELETED" -> "🗑️";
            case "PROJECT_CREATED" -> "🗂️";
            case "PROJECT_ARCHIVED" -> "📦";
            case "PROFILE_UPDATED" -> "👤";
            case "PASSWORD_CHANGED" -> "🔒";
            default -> "🔔";
        };
    }
}
