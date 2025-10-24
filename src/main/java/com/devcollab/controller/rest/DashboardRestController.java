package com.devcollab.controller.rest;

import com.devcollab.dto.response.*;
import com.devcollab.service.system.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pm/dashboard")
@RequiredArgsConstructor
public class DashboardRestController {

    private final DashboardService dashboardService;

    /**
     * Trả dữ liệu tổng quan (Project Summary)
     * Hỗ trợ filter qua query param ?range=week|month|6months|year
     */
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    @GetMapping("/summary")
    public ApiResponse<ProjectSummaryDTO> getSummary(
            @RequestParam(defaultValue = "6months") String range) {

        return ApiResponse.success(dashboardService.getProjectSummary(range));
    }

    /**
     * Trả dữ liệu biểu đồ Performance (Achieved vs Target)
     * Hỗ trợ filter qua query param ?range=week|month|6months|year
     */
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    @GetMapping("/performance")
    public ApiResponse<ProjectPerformanceDTO> getPerformance(
            @RequestParam(defaultValue = "6months") String range) {

        return ApiResponse.success(dashboardService.getProjectPerformance(range));
    }
}
