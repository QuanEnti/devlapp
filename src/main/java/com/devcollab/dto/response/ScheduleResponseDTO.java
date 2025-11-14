package com.devcollab.dto.response;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class ScheduleResponseDTO {
    private Long scheduleId;
    private Long projectId;
    private String title;
    private String note;
    private String meetingLink;
    private Instant datetime;
    private Instant createdAt;
}

