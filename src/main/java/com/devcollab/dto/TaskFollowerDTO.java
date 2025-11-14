package com.devcollab.dto;

import com.devcollab.domain.TaskFollower;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskFollowerDTO {
    private Long taskId;
    private Long userId;
    private String name;

    private String userEmail;
    private String avatarUrl;
    private LocalDateTime followedAt;

    public static TaskFollowerDTO fromEntity(TaskFollower follower) {
        return new TaskFollowerDTO(
                follower.getTask().getTaskId(),
                follower.getUser().getUserId(),
                follower.getUser().getName(),
                follower.getUser().getEmail(),
                follower.getUser().getAvatarUrl(),
                follower.getFollowedAt());
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public LocalDateTime getFollowedAt() {
        return followedAt;
    }

    public void setFollowedAt(LocalDateTime followedAt) {
        this.followedAt = followedAt;
    }
}
