package com.devcollab.dto;

import com.devcollab.domain.Task;
import lombok.*;

import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {

    private Long id; 
    private String title; 
    private String status; 
    private String priority;
    private String startDate; 
    private String endDate; 
    private String creatorName; 
    private String assigneeName;
    private String assigneeAvatar;
    private String columnName; 
    private String projectName; 

    public static TaskDTO fromEntity(Task task) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        return TaskDTO.builder()
                .id(task.getTaskId())
                .title(task.getTitle())
                .status(task.getStatus())
                .priority(task.getPriority())
                .startDate(task.getCreatedAt() != null ? task.getCreatedAt().format(fmt) : "")
                .endDate(task.getDeadline() != null ? task.getDeadline().format(fmt) : "")
                .creatorName(task.getCreatedBy() != null ? task.getCreatedBy().getName() : "Unknown")
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getName() : "Unassigned")
                .assigneeAvatar(task.getAssignee() != null ? task.getAssignee().getAvatarUrl() : null)
                .columnName(task.getColumn() != null ? task.getColumn().getName() : "")
                .projectName(task.getProject() != null ? task.getProject().getName() : "")
                .build();
    }
}
