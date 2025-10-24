package com.devcollab.service.system;

import com.devcollab.dto.response.ProjectSummaryDTO;
import com.devcollab.dto.response.ProjectPerformanceDTO;

public interface DashboardService {
    ProjectSummaryDTO getProjectSummary(String range);

    ProjectPerformanceDTO getProjectPerformance(String range);
}
