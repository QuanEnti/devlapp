package com.devcollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberPerformanceDTO {
    private Long userId;
    private String name;
    private String email;
    private long totalTasks;
    private long completedTasks;
    private long onTimeTasks;
    private long lateTasks;
    private double avgDelayHours;
    private int priorityPoints;
    private double performanceScore; // calculated
}
