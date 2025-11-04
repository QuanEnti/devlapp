package com.devcollab.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "[Activity]")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long activityId;

    @ManyToOne
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType; 

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 40)
    private String action; 

    @Column(name = "data_json", columnDefinition = "NVARCHAR(MAX)")
    private String dataJson; 

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Activity() {
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDataJson() {
        return dataJson;
    }

    public void setDataJson(String dataJson) {
        this.dataJson = dataJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
