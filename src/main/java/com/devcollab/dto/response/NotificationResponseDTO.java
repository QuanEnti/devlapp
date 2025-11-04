package com.devcollab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NotificationResponseDTO {
    private Long notificationId;  // Make sure this matches the JavaScript
    private String type;          // Add this field
    private String title;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private Long referenceId;
    private String content;
}
