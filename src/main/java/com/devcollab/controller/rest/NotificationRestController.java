package com.devcollab.controller.rest;

import com.devcollab.domain.Notification;
import com.devcollab.dto.response.NotificationResponseDTO;
import com.devcollab.service.system.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getNotifications(Authentication auth) {
        if (auth == null)
            return ResponseEntity.status(401).body("Unauthenticated");

        String email = extractEmail(auth);
        log.info("📩 GET /api/notifications for {}", email);

        List<Notification> notifications = notificationService.getNotificationsByUser(email);

        List<NotificationResponseDTO> responseList =
                notifications.stream()
                        .map(this::mapToResponseDTO)
                        .filter(Objects::nonNull)
                        .toList();

        return ResponseEntity.ok(responseList);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable("id") Long id, Authentication auth) {
        if (auth == null)
            return ResponseEntity.status(401).body("Unauthenticated");

        String email = extractEmail(auth);
        boolean updated = notificationService.markAsRead(id, email);
        if (!updated)
            return ResponseEntity.status(403)
                    .body("You cannot mark someone else's notification as read.");

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication auth) {
        if (auth == null)
            return ResponseEntity.status(401).body("Unauthenticated");

        String email = extractEmail(auth);
        int updated = notificationService.markAllAsRead(email);

        return ResponseEntity.ok("Đã đánh dấu " + updated + " thông báo là đã đọc.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable("id") Long id, Authentication auth) {
        if (auth == null)
            return ResponseEntity.status(401).body("Unauthenticated");

        String email = extractEmail(auth);
        // Check ownership before deleting
        List<Notification> notifications = notificationService.getNotificationsByUser(email);
        boolean isOwner = notifications.stream()
                .anyMatch(n -> n.getNotificationId().equals(id));
        
        if (!isOwner) {
            return ResponseEntity.status(403)
                    .body("You cannot delete someone else's notification.");
        }
        
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> countUnread(Authentication auth) {
        if (auth == null)
            return ResponseEntity.status(401).body("Unauthenticated");

        String email = extractEmail(auth);
        int count = notificationService.countUnread(email);
        return ResponseEntity.ok(count);
    }

    private String extractEmail(Authentication auth) {
        if (auth == null)
            return null;
        if (auth.getPrincipal() instanceof DefaultOidcUser oidcUser)
            return oidcUser.getEmail();
        return auth.getName();
    }


    private NotificationResponseDTO mapToResponseDTO(Notification n) {
        try {
            if (n == null)
                return null;

            String type = n.getType() != null ? n.getType().trim().toUpperCase() : "GENERAL";
            String title = n.getTitle() != null ? n.getTitle() : "Thông báo mới";
            String message = n.getMessage() != null ? n.getMessage() : "Bạn có thông báo mới.";

            String link = (n.getLink() == null || n.getLink().isBlank()) ? "#" : n.getLink();

            String senderName = (n.getSender() != null && n.getSender().getName() != null)
                    ? n.getSender().getName()
                    : "Hệ thống";

            String senderAvatar = n.getSender() != null ? n.getSender().getAvatarUrl() : null;

            return NotificationResponseDTO.builder().id(n.getNotificationId()).type(type)
                    .title(title).message(message).status(n.getStatus()).createdAt(n.getCreatedAt())
                    .referenceId(n.getReferenceId()).link(link).icon(mapIcon(type))
                    .senderName(senderName).senderAvatar(senderAvatar).build();

        } catch (Exception e) {
            log.warn("⚠ mapToResponseDTO() error for {}: {}",
                    n != null ? n.getNotificationId() : "null", e.getMessage());
            return null;
        }
    }

    private String mapIcon(String type) {
        return switch (type) {
            case "PROJECT_CREATED" -> "🗂️";
            case "SCHEDULE_CREATED" -> "📅";
            case "PROJECT_ARCHIVED" -> "📦";
            case "MEMBER_ADDED" -> "👥";
            case "PROJECT_MEMBER_ROLE_UPDATED" -> "👤";

            case "TASK_MEMBER_ADDED" -> "👤";
            case "TASK_MEMBER_REMOVED" -> "❌";
            case "TASK_COMMENTED" -> "💬";
            case "TASK_COMMENT_MENTION" -> "📣";
            case "PROJECT_COMMENT_MENTION" -> "📢";
            case "TASK_DUE_SOON" -> "⏰";
            case "TASK_FOLLOWED" -> "⭐";

            case "PROJECT_LINK_REGENERATED" -> "🔗";

            case "JOIN_REQUEST_RECEIVED" -> "📩";
            case "JOIN_REQUEST_APPROVED" -> "✅";
            case "JOIN_REQUEST_REJECTED" -> "❌";

            case "PASSWORD_CHANGED" -> "🔑";
            case "PROFILE_UPDATED" -> "⚙️";

            default -> "🔔";
        };
    }
}
