package com.devcollab.controller.rest;

import com.devcollab.domain.Notification;
import com.devcollab.domain.Project;
import com.devcollab.dto.response.NotificationResponseDTO;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.service.system.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final NotificationService notificationService;
    private final ProjectRepository projectRepository; // 👉 Thêm repository này

    @GetMapping
    public List<NotificationResponseDTO> getNotifications(Authentication auth) {
        String email = auth.getName();
        List<Notification> notifications = notificationService.getNotificationsByUser(email);

        return notifications.stream().map(n -> {
            String title;
            String message;

            String projectName = projectRepository.findById(n.getReferenceId())
                    .map(Project::getName)
                    .orElse("Không xác định");

            // Generate title & message
            switch (n.getType()) {
                case "PROJECT_CREATED":
                    title = "Dự án mới được tạo";
                    message = "Bạn đã tạo dự án: " + projectName;
                    break;
                case "MEMBER_ADDED":
                    title = "Bạn đã được thêm vào dự án";
                    message = "Bạn đã được thêm vào dự án: " + projectName;
                    break;
                case "TASK_ASSIGNED":
                    title = "Task mới được giao";
                    message = "Bạn được giao một task trong dự án: " + projectName;
                    break;
                default:
                    title = "Thông báo mới";
                    message = "Bạn có thông báo mới.";
            }

            // ✅ Build redirect URL
            String redirectUrl = null;
            if (n.getReferenceId() != null) {
                if (n.getType().startsWith("PROJECT")) {
                    redirectUrl = "/view/project/" + n.getReferenceId() + "/tasks";
                } else if (n.getType().startsWith("TASK")) {
                    redirectUrl = "/view/task/" + n.getReferenceId();
                }
            }

            return new NotificationResponseDTO(
                    n.getNotificationId(),
                    title,
                    message,
                    n.getStatus(),
                    n.getCreatedAt(),
                    n.getReferenceId(),
                    redirectUrl // ✅ Thêm vào đây
            );
        }).collect(Collectors.toList());
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable("id") Long notificationId) {
        notificationService.markAsRead(notificationId);
    }
}
