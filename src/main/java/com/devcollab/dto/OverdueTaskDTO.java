package com.devcollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverdueTaskDTO {
    private Long taskId;
    private String title;
    private String assigneeName;
    private String assigneeEmail;
    private LocalDateTime deadline;
    private Long daysOverdue;
}

