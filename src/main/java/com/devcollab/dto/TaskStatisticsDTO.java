package com.devcollab.dto;

import java.util.Map;

public class TaskStatisticsDTO {
    private Map<String, Long> statusCount;
    private Map<String, Double> statusPercentages;
    private long totalTasks;

    public TaskStatisticsDTO(Map<String, Long> statusCount,
                             Map<String, Double> statusPercentages,
                             long totalTasks) {
        this.statusCount = statusCount;
        this.statusPercentages = statusPercentages;
        this.totalTasks = totalTasks;
    }

    // Getters and setters
    public Map<String, Long> getStatusCount() { return statusCount; }
    public void setStatusCount(Map<String, Long> statusCount) { this.statusCount = statusCount; }

    public Map<String, Double> getStatusPercentages() { return statusPercentages; }
    public void setStatusPercentages(Map<String, Double> statusPercentages) { this.statusPercentages = statusPercentages; }

    public long getTotalTasks() { return totalTasks; }
    public void setTotalTasks(long totalTasks) { this.totalTasks = totalTasks; }
}
