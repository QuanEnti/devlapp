package com.devcollab.dto.taskDto;

import com.devcollab.domain.Task;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskListItemDto {
    private Long taskId;
    private String title;
    private String status;
    private String priority;
    private LocalDateTime createdAt;
    private UserSummaryDto assignee; // Biết task này được giao cho ai

    /**
     * Factory method để chuyển đổi từ Entity Task
     */
    public static TaskListItemDto fromEntity(Task task) {
        return new TaskListItemDto(
                task.getTaskId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getCreatedAt(),
                UserSummaryDto.fromEntity(task.getAssignee()) // Dùng lại DTO tóm tắt user
        );
    }

}
