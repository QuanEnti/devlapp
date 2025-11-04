package com.devcollab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO trả về cho client (qua WebSocket hoặc REST API),
 * chứa thông tin chi tiết của thông báo hiển thị trên UI.
 * 
 * Ví dụ hiển thị:
 * 🖼️ [Avatar] Nguyễn Tiến Quân đã thêm bạn vào công việc "Thiết kế UI"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDTO {
    private Long id; 
    private String type; 
    private String title; // 🔹 Tiêu đề ngắn (VD: "Công việc sắp đến hạn")
    private String message; // 🔹 Nội dung chi tiết (VD: "Công việc 'Thiết kế UI' sắp đến hạn vào 05/11")
    private String status; // 🔹 "read" | "unread"
    private LocalDateTime createdAt; // 🔹 Thời điểm tạo thông báo
    private Long referenceId; // 🔹 ID của thực thể liên quan (Task, Project, User,...)
    private String link; // 🔹 Link điều hướng khi click (VD: /projects/12/tasks/45)
    private String icon; // 🔹 Icon đại diện loại thông báo (📋, 🕓, 👥,...)

    // 🧩 Thông tin người gửi (giúp hiển thị UI đẹp như Trello)
    private String senderName; // 👤 Tên người gửi (VD: "Nguyễn Tiến Quân")
    private String senderAvatar; // 🖼️ Ảnh đại diện (URL)
}