package com.devcollab.controller.rest;

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

    public AdminDashboardRestController(UserRepository userRepo, ProjectRepository projectRepo) {
        this.userRepo = userRepo;
        this.projectRepo = projectRepo;
    }

    @GetMapping("/api/admin/dashboard")
    public Map<String, Object> getDashboardData() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalUsers", userRepo.findAll().toArray().length);
        stats.put("activeUsers", userRepo.countByStatus("active"));
        stats.put("totalProjects", projectRepo.count());
        stats.put("violatedProjects", projectRepo.countByStatus("violated"));

        // Example chart data: user registrations per month
        List<Object[]> raw = userRepo.countUsersByMonth();
        List<List<Object>> registrations = new ArrayList<>();
        for (Object[] row : raw) {
            registrations.add(List.of(row[0], row[1]));
        }
        stats.put("registrations", registrations);

        return stats;
    }
}

