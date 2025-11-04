package com.devcollab.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "[TaskFollower]")
public class TaskFollower {

    @EmbeddedId
    private TaskFollowerId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("taskId")
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "followed_at", nullable = false)
    private LocalDateTime followedAt = LocalDateTime.now();

    public TaskFollower() {
    }

    public TaskFollower(Task task, User user) {
        this.task = task;
        this.user = user;
        this.id = new TaskFollowerId(task.getTaskId(), user.getUserId());
        this.followedAt = LocalDateTime.now();
    }

    public TaskFollowerId getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getFollowedAt() {
        return followedAt;
    }

    public void setId(TaskFollowerId id) {
        this.id = id;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setFollowedAt(LocalDateTime followedAt) {
        this.followedAt = followedAt;
    }
}
