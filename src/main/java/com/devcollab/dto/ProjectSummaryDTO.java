package com.devcollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class ProjectSummaryDTO {
    private Long projectId;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate dueDate;
    private String status;
    private String priority;
}

