package com.devcollab.dto.userTaskDto;

import java.time.LocalDateTime;

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