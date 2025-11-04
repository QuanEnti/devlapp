package com.devcollab.controller.rest;

import com.devcollab.domain.Notification;
import com.devcollab.domain.Project;
import com.devcollab.domain.Task;
import com.devcollab.dto.response.NotificationResponseDTO;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.service.system.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final NotificationService notificationService;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    // ======================================================
    // 🔔 Lấy danh sách thông báo của user hiện tại
    // ======================================================
    @GetMapping
    public ResponseEntity<?> getNotifications(Authentication auth) {
        if (auth == null)
            return ResponseEntity.status(401).body("Unauthenticated");

        String email = extractEmail(auth);
        log.info("📩 GET /api/notifications for {}", email);

        try {
            List<Notification> notifications = notificationService.getNotificationsByUser(email);
            if (notifications == null || notifications.isEmpty())
                return ResponseEntity.ok(List.of());

            List<NotificationResponseDTO> responseList = notifications.stream()
                    .map(this::mapToResponseDTO)
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            log.error("❌ Error loading notifications: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // ======================================================
    // 📖 Đánh dấu 1 thông báo là đã đọc
    // ======================================================
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable("id") Long id, Authentication auth) {
        if (auth == null)
            return ResponseEntity.status(401).body("Unauthenticated");

        String email = extractEmail(auth);
        log.info("📖 PUT /api/notifications/{}/read by {}", id, email);

        try {
            boolean updated = notificationService.markAsRead(id, email);
            if (!updated)
                return ResponseEntity.status(403)
                        .body("You cannot mark someone else's notification as read.");
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("❌ markAsRead() failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // ======================================================
    // 📬 Đánh dấu tất cả thông báo là đã đọc
    // ======================================================
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication auth) {
        if (auth == null)
            return ResponseEntity.status(401).body("Unauthenticated");

        String email = extractEmail(auth);
        try {
            int updated = notificationService.markAllAsRead(email);
            return ResponseEntity.ok("✅ Marked " + updated + " notifications as read.");
        } catch (Exception e) {
            log.error("❌ markAllAsRead() failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // ======================================================
    // 🗑️ Xóa thông báo
    // ======================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable("id") Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    // ======================================================
    // 🔹 Đếm số thông báo chưa đọc
    // ======================================================
    @GetMapping("/unread-count")
    public ResponseEntity<?> countUnread(Authentication auth) {
        if (auth == null)
            return ResponseEntity.status(401).body("Unauthenticated");

        String email = extractEmail(auth);
        int count = notificationService.countUnread(email);
        return ResponseEntity.ok(count);
    }

    // ======================================================
    // 🧠 Helper: lấy email từ Auth (Local / Google)
    // ======================================================
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
            String link = n.getLink() != null ? n.getLink() : "#";
            String projectName = "Không xác định";

            // 🔹 Mapping project/task link an toàn
            if (n.getReferenceId() != null && type.startsWith("PROJECT_")) {
                projectName = projectRepository.findById(n.getReferenceId())
                        .map(Project::getName)
                        .orElse("Không xác định");

                // 🧩 Nếu DB đã có link hợp lệ thì giữ nguyên, chỉ fallback nếu null
                if (link == null || link.equals("#") || link.isBlank()) {
                    link = "/view/pm/project/board?projectId=" + n.getReferenceId();
                }
                    }
                    else if (n.getReferenceId() != null && type.startsWith("TASK_")) {
                                    Task task = taskRepository.findById(n.getReferenceId()).orElse(null);
                if (task != null && task.getProject() != null) {
                    projectName = task.getProject().getName();
                    link = "/projects/" + task.getProject().getProjectId()
                            + "/tasks/" + task.getTaskId();
                }
            }

            if (message.contains("{project}"))
                message = message.replace("{project}", projectName);

            // ✅ Lấy thông tin người gửi (sender) thay vì hardcode “Hệ thống”
            String senderName = "Hệ thống";
            String senderAvatar = null;
            if (n.getSender() != null) {
                senderName = n.getSender().getName() != null ? n.getSender().getName() : "Hệ thống";
                senderAvatar = n.getSender().getAvatarUrl();
            }

            return NotificationResponseDTO.builder()
                    .id(n.getNotificationId())
                    .type(type)
                    .title(title)
                    .message(message)
                    .status(n.getStatus())
                    .createdAt(n.getCreatedAt())
                    .referenceId(n.getReferenceId())
                    .link(link)
                    .icon(mapIcon(type))
                    .senderName(senderName)
                    .senderAvatar(senderAvatar)
                    .build();

        } catch (Exception e) {
            log.warn("⚠️ mapToResponseDTO() error for {}: {}", n.getNotificationId(), e.getMessage());
            return null;
        }
    }

    // ======================================================
    // 🧭 Helper: type → icon
    // ======================================================
    private String mapIcon(String type) {
        return switch (type) {
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
