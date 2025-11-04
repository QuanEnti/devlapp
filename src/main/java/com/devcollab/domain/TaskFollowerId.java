package com.devcollab.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TaskFollowerId implements Serializable {

    private Long taskId;
    private Long userId;

    public TaskFollowerId() {
    }

    public TaskFollowerId(Long taskId, Long userId) {
        this.taskId = taskId;
        this.userId = userId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TaskFollowerId that))
            return false;
        return Objects.equals(taskId, that.taskId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, userId);
    }
}
