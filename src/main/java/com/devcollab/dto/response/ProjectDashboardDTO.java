package com.devcollab.dto.response;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDashboardDTO {
    private Long projectId;

    private long totalTasks;
    private long openTasks;
    private long inProgressTasks;
    private long reviewTasks;
    private long doneTasks;
    private long inProgress;
    private long overdueTasks;
    private BigDecimal percentDone; 
}
