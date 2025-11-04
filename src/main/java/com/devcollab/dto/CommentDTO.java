package com.devcollab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO dùng để trả về dữ liệu comment kèm thông tin người dùng và reply.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {

    private Long commentId;
    private Long taskId;
    private Long parentId;
    private String content;

    // 🧩 Dùng để xác định ai là người đăng
    private Long userId;
    private String userName;
    private String userEmail; // 🔹 Thêm trường này để frontend so sánh quyền
    private String userAvatar;

    private LocalDateTime createdAt;

    // Danh sách phản hồi (reply)
    @Builder.Default
    private List<CommentDTO> replies = new ArrayList<>();

    // ✅ Constructor custom khi dùng query mapping
    public CommentDTO(Long commentId, Long taskId, Long parentId, String content,
            Long userId, String userName, String userEmail,
            String userAvatar, LocalDateTime createdAt) {
        this.commentId = commentId;
        this.taskId = taskId;
        this.parentId = parentId;
        this.content = content;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail; // ⚙️ Map luôn từ entity
        this.userAvatar = userAvatar;
        this.createdAt = createdAt;
    }
}
