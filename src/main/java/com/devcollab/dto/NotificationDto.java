package com.devcollab.dto;
import com.devcollab.domain.Notification;

import java.time.LocalDateTime;

public class NotificationDto {
    private Long id;
    private String type;
    private String content;
    private String status;
    private LocalDateTime createdAt;
    private Long userId;
    private String userName;

    public NotificationDto(Notification n) {
        this.id = n.getNotificationId();
        this.type = n.getType();
        this.content = n.getContent();
        this.status = n.getStatus();
        this.createdAt = n.getCreatedAt();
        this.userId = n.getUser() != null ? n.getUser().getUserId() : null;
        this.userName = n.getUser() != null ? n.getUser().getName() : null;
    }

}
