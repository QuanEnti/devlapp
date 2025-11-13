package com.devcollab.controller.rest;

import com.devcollab.service.core.PaymentOrderService;
import com.devcollab.service.core.ProjectReportService;
import com.devcollab.service.core.ProjectService;
import com.devcollab.service.core.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class AdminDashboardRestController {

    private final UserService userService;
    private final ProjectService projectService;
    private final ProjectReportService projectReportService;
    private final PaymentOrderService paymentService;

    @GetMapping("/api/admin/dashboard")
    public Map<String, Object> getDashboardData() {
        Map<String, Object> stats = new HashMap<>();

        // 🔹 Basic counts
        long totalUsers = userService.getAll().size();
        long activeUsers = userService.countByStatus("active");
        long totalProjects = projectService.countAll();
        long activeProjects = projectService.countByStatus("Active");
        long archivedProjects = projectService.countByStatus("Archived");

        // 🔹 Violated projects (pending + reviewed reports)
        long violatedProjects = projectReportService.countViolatedProjects();

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("totalProjects", totalProjects);
        stats.put("violatedProjects", violatedProjects);
        stats.put("activeProjects", activeProjects);
        stats.put("archivedProjects", archivedProjects);

        // 🔹 Revenue chart data (last 6 months)
        stats.put("revenue", paymentService.getRevenueByMonth());

        return stats;
    }
}
