package com.devcollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDTO {
    private Long activityId;
    private String entityType;
    private Long entityId;
    private String action;
    private Long actorId;
    private String actorName;
    private String actorAvatar;
    private String dataJson;
    private LocalDateTime createdAt;
}
