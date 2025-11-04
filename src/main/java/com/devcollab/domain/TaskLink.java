package com.devcollab.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "[TaskLink]")
public class TaskLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_id")
    private Long linkId;

    @ManyToOne
    @JoinColumn(name = "from_task_id", nullable = false)
    private Task fromTask;

    @ManyToOne
    @JoinColumn(name = "to_task_id", nullable = false)
    private Task toTask;

    @Column(name = "link_type", nullable = false, length = 20)
    private String linkType; // BLOCKS | RELATES | DUPLICATES

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public TaskLink() {
    }

    public Long getLinkId() {
        return linkId;
    }

    public void setLinkId(Long linkId) {
        this.linkId = linkId;
    }

    public Task getFromTask() {
        return fromTask;
    }

    public void setFromTask(Task fromTask) {
        this.fromTask = fromTask;
    }

    public Task getToTask() {
        return toTask;
    }

    public void setToTask(Task toTask) {
        this.toTask = toTask;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
