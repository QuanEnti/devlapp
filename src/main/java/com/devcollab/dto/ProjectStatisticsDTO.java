package com.devcollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatisticsDTO {
    private Long totalTasks;
    private Double totalHours; // Tổng số giờ của tất cả task
    private Long completedTasks;
    private Long incompleteTasks;
    private Long overdueTasks;
    private List<MemberTaskCountDTO> topMembers; // Nhân viên làm nhiều task nhất
    private List<OverdueTaskDTO> overdueTaskDetails; // Chi tiết các task bị overdue
}


