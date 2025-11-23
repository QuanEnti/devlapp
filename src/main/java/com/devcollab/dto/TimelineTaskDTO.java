package com.devcollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimelineTaskDTO {
    private Long taskId;
    private Long projectId;
    private String projectName;
    private String title;
    private String deadline;
    private String status;
}

