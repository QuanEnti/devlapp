package com.devcollab.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskReviewDTO {
    private Long id;
    private String title;
    private String status;
    private String priority;
    private String deadline;
    private String assigneeName;
    private String assigneeAvatar;
    private String descriptionMd;
    private String assgineeName;

    // ✅ Specific for review page - includes followers
    private List<TaskFollowerDTO> followers;

    public static TaskReviewDTO fromEntity(com.devcollab.domain.Task task, List<TaskFollowerDTO> followers) {
        DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        return TaskReviewDTO.builder()
                .id(task.getTaskId())
                .title(task.getTitle())
                .status(task.getStatus())
                .priority(task.getPriority())
                .deadline(task.getDeadline() != null ? task.getDeadline().format(iso) : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getName() : null)
                .assigneeAvatar(task.getAssignee() != null ? task.getAssignee().getAvatarUrl() : null)
                .descriptionMd(task.getDescriptionMd())
                .assgineeName(task.getAssignee() != null ? task.getAssignee().getName() : null)
                .followers(followers != null ? followers : List.of())
                .build();
    }
}