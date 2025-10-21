package com.devcollab.dto.taskDto;

import com.devcollab.dto.UserProfileDto.ProjectSummaryDto;
import com.devcollab.domain.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskDetailDto {
    // Thông tin cơ bản
    private Long taskId;
    private String title;
    private String descriptionMd;
    private String status;
    private String priority;
    private LocalDateTime deadline; // UI của bạn là "Due Date"

    // Thông tin quan hệ (khớp với UI)
    private UserSummaryDto owner; // UI của bạn là "Owner" (CreatedBy)
    private UserSummaryDto assignee; // UI của bạn là "Assignee"
    private ProjectSummaryDto project; // UI của bạn có "Project | Task ID-..."

    // Các danh sách liên quan (khớp với UI)
    private List<AttachmentDto> attachments;
    private List<CommentDto> comments;

     //(Tôi vẫn giữ 2 trường này, phòng khi bạn cần)
    private Set<LabelDto> labels;
    private List<CheckListDto> checklists;


    public static TaskDetailDto fromEntity(Task task) {
        return TaskDetailDto.builder()
                .taskId(task.getTaskId())
                .title(task.getTitle())
                .descriptionMd(task.getDescriptionMd())
                .status(task.getStatus())
                .priority(task.getPriority())
                .deadline(task.getDeadline())

                // Map quan hệ
                .owner(UserSummaryDto.fromEntity(task.getCreatedBy())) // "Owner"
                .assignee(UserSummaryDto.fromEntity(task.getAssignee()))
                .project(new ProjectSummaryDto(
                        task.getProject().getProjectId(),
                        task.getProject().getName()
                ))

                // Map danh sách
                // (Yêu cầu bạn phải thêm @OneToMany vào Task.java)
                .attachments(task.getAttachments().stream()
                        .map(AttachmentDto::fromEntity)
                        .collect(Collectors.toList()))

                .comments(task.getComments().stream()
                        .map(CommentDto::fromEntity)
                        .collect(Collectors.toList()))

                .labels(task.getLabels().stream()
                        .map(LabelDto::fromEntity)
                        .collect(Collectors.toSet()))

                .checklists(task.getChecklists().stream()
                        .map(CheckListDto::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }
}
