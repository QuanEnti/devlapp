package com.devcollab.controller.rest;

import com.devcollab.repository.ProjectReportRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AdminDashboardRestController {

    private final UserRepository userRepo;
    private final ProjectRepository projectRepo;

    private final ProjectReportRepository projectReportRepo;

    public AdminDashboardRestController(UserRepository userRepo, ProjectRepository projectRepo, ProjectReportRepository projectReportRepo) {
        this.userRepo = userRepo;
        this.projectRepo = projectRepo;
        this.projectReportRepo = projectReportRepo;
    }

    @GetMapping("/api/admin/dashboard")
    public Map<String, Object> getDashboardData() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalUsers", userRepo.findAll().toArray().length);
        stats.put("activeUsers", userRepo.countByStatus("active"));
        stats.put("totalProjects", projectRepo.count());
        long pendingCount = projectReportRepo.countByStatus("pending");
        long reviewedCount = projectReportRepo.countByStatus("reviewed");
        long violatedProjects = pendingCount + reviewedCount;

        stats.put("violatedProjects", violatedProjects);

        List<Object[]> raw = userRepo.countUsersByMonth();
        List<List<Object>> registrations = new ArrayList<>();
        for (Object[] row : raw) {
            registrations.add(List.of(row[0], row[1]));
        }
        stats.put("registrations", registrations);

        return stats;
    }
}