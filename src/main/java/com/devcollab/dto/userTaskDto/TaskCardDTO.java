package com.devcollab.dto.userTaskDto;

import java.time.LocalDateTime;

/**
 * DTO này đại diện cho một "Thẻ Task" (Task Card)
 * được hiển thị trong danh sách "My Tasks".
 * Tên trường phải khớp 100% với tên cột (aliases) trong [sp_GetUserTasks].
 */
public record TaskCardDTO(
    Long id,
    String title,
    String status,
    String priority,
    LocalDateTime createdAt,
    LocalDateTime deadline,
    String creatorName,
    String assigneeAvatarUrl,
    int checklistCount,
    int commentCount
) {}
