package com.devcollab.dto;

import com.devcollab.domain.Task;
import lombok.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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
    private Long projectId;
    private Long columnId;
    private String recurring;
    private String reminder;
    private String descriptionMd;
    private String deadline;
    private List<LabelDTO> labels;
    
    public static TaskDTO fromEntity(Task task) {
        // ✅ Dùng ISO 8601 format – frontend có thể parse bằng new Date() hoặc
        // datetime-local
        DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        return TaskDTO.builder()
                .id(task.getTaskId())
                .title(task.getTitle())
                .status(task.getStatus())
                .priority(task.getPriority())

                // ✅ ISO format an toàn, ví dụ: "2025-10-31T18:38:00"
                .startDate(task.getStartDate() != null ? task.getStartDate().format(iso) : "")
                .endDate(task.getDeadline() != null ? task.getDeadline().format(iso) : "")

                .creatorName(task.getCreatedBy() != null ? task.getCreatedBy().getName() : "Unknown")
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getName() : "Unassigned")
                .assigneeAvatar(task.getAssignee() != null ? task.getAssignee().getAvatarUrl() : null)
                .columnName(task.getColumn() != null ? task.getColumn().getName() : "")
                .projectName(task.getProject() != null ? task.getProject().getName() : "")
                .projectId(task.getProject() != null ? task.getProject().getProjectId() : null)
                .columnId(task.getColumn() != null ? task.getColumn().getColumnId() : null)
                .recurring(task.getRecurring())
                .reminder(task.getReminder())
                .descriptionMd(task.getDescriptionMd())
                .deadline(task.getDeadline() != null ? task.getDeadline().format(iso) : "")
                 .labels(task.getLabels() != null
                        ? task.getLabels().stream()
                              .map(l -> new LabelDTO(l.getLabelId(), l.getName(), l.getColor()))
                              .collect(Collectors.toList())
                        : List.of())
                .build();
    }

    public Task toEntity() {
        Task task = new Task();
        task.setTitle(this.title);
        task.setPriority(this.priority != null ? this.priority : "MEDIUM");
        task.setDescriptionMd(this.descriptionMd);

        // ✅ Parse ISO datetime (2025-10-31T18:38)
        if (this.startDate != null && !this.startDate.isBlank()) {
            try {
                task.setStartDate(LocalDateTime.parse(this.startDate));
            } catch (Exception ignored) {
            }
        }

        if (this.deadline != null && !this.deadline.isBlank()) {
            try {
                task.setDeadline(LocalDateTime.parse(this.deadline));
            } catch (Exception ignored) {
            }
        }

        task.setRecurring(this.recurring != null ? this.recurring : "Never");
        task.setReminder(this.reminder != null ? this.reminder : "Never");

        return task;
    }

    public static TaskDTO forQuickCreate(Task task) {
        return TaskDTO.builder()
                .id(task.getTaskId())
                .title(task.getTitle())
                .columnId(task.getColumn() != null ? task.getColumn().getColumnId() : null)
                .projectId(task.getProject() != null ? task.getProject().getProjectId() : null)
                .creatorName(task.getCreatedBy() != null ? task.getCreatedBy().getName() : "Unknown")
                .build();
    }
}
