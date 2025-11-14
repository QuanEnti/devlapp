package com.devcollab.dto;

import com.devcollab.domain.Task;
import com.devcollab.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class TaskDetailDTO {
    private Long id;
    private String title;
    private String descriptionMd;
    private String priority;
    private String status;
    private LocalDateTime deadline;

    // Assignee information
    private Long assigneeId;
    private String assigneeName;
    private String assigneeAvatar;

    private List<TaskFollowerDTO> followers;
    private List<CommentDTO> comments;
    private List<AttachmentDTO> attachments;

    public static TaskDetailDTO fromEntity(Task task, List<TaskFollowerDTO> followers,
                                           List<CommentDTO> comments, List<AttachmentDTO> attachments) {
        TaskDetailDTO dto = new TaskDetailDTO();
        dto.setId(task.getTaskId());
        dto.setTitle(task.getTitle());
        dto.setDescriptionMd(task.getDescriptionMd());
        dto.setPriority(task.getPriority() != null ? task.getPriority() : null);
        dto.setStatus(task.getStatus() != null ? task.getStatus() : null);
        dto.setDeadline(task.getDeadline());
        dto.setFollowers(followers != null ? followers : new ArrayList<>());
        dto.setComments(comments != null ? comments : new ArrayList<>());
        dto.setAttachments(attachments != null ? attachments : new ArrayList<>());

        // Safely populate assignee information
        User assignee = task.getCreatedBy();
        if (assignee != null) {
            System.out.println("Assignee found: " + assignee.getName());
            dto.setAssigneeId(assignee.getUserId());

            // Use name if available, otherwise fallback to email
            String assigneeName = assignee.getName();
            if (assigneeName == null || assigneeName.trim().isEmpty()) {
                assigneeName = assignee.getEmail();
            }
            dto.setAssigneeName(assigneeName);

            // Set avatar with default fallback
            dto.setAssigneeAvatar(assignee.getAvatarUrl() != null ?
                    assignee.getAvatarUrl() : "/photo/default-avatar.png");
        } else {
            System.out.println("No assignee found for task: " + task.getTaskId());
            dto.setAssigneeId(null);
            dto.setAssigneeName("Unassigned");
            dto.setAssigneeAvatar("/photo/default-avatar.png");
        }

        return dto;
    }

    // Alternative constructor without pre-loaded collections
    public static TaskDetailDTO fromEntity(Task task) {
        return fromEntity(task, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescriptionMd() {
        return descriptionMd;
    }

    public void setDescriptionMd(String descriptionMd) {
        this.descriptionMd = descriptionMd;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public String getAssigneeAvatar() {
        return assigneeAvatar;
    }

    public void setAssigneeAvatar(String assigneeAvatar) {
        this.assigneeAvatar = assigneeAvatar;
    }

    public List<TaskFollowerDTO> getFollowers() {
        return followers;
    }

    public void setFollowers(List<TaskFollowerDTO> followers) {
        this.followers = followers;
    }

    public List<CommentDTO> getComments() {
        return comments;
    }

    public void setComments(List<CommentDTO> comments) {
        this.comments = comments;
    }

    public List<AttachmentDTO> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentDTO> attachments) {
        this.attachments = attachments;
    }

    @Override
    public String toString() {
        return "TaskDetailDTO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", priority='" + priority + '\'' +
                ", status='" + status + '\'' +
                ", assigneeName='" + assigneeName + '\'' +
                ", followersCount=" + (followers != null ? followers.size() : 0) +
                ", commentsCount=" + (comments != null ? comments.size() : 0) +
                ", attachmentsCount=" + (attachments != null ? attachments.size() : 0) +
                '}';
    }
}