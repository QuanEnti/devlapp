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
    private String userName;
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
}
