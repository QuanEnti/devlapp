package com.devcollab.dto;


import com.devcollab.domain.Task;
import lombok.*;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UserTaskViewDTO {
    private Long id;
    private Long projectId;
    private String title;
    private String projectName;
    private String priority;
    private String status;
    private String deadline;

    public static UserTaskViewDTO fromEntity(Task task) {
        DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return UserTaskViewDTO.builder()
                .id(task.getTaskId())
                .title(task.getTitle())
                .projectId(task.getProject() != null ? task.getProject().getProjectId() : null)
                .projectName(task.getProject() != null ? task.getProject().getName() : "-")
                .priority(task.getPriority())
                .status(task.getStatus())
                .deadline(task.getDeadline() != null ? task.getDeadline().format(iso).substring(0, 10) : null)
                .build();
    }

}

