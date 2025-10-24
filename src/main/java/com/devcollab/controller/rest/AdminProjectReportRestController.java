package com.devcollab.controller.rest;

import com.devcollab.domain.ProjectReport;
import com.devcollab.domain.Project;
import com.devcollab.domain.Notification;
import com.devcollab.dto.ProjectReportDto;
import com.devcollab.dto.UserReportDto;
import com.devcollab.repository.ProjectReportRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.NotificationRepository;
import com.devcollab.service.core.ProjectReportService;
import com.devcollab.service.core.ProjectService;
import com.devcollab.service.system.ActivityService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/project-reports")
public class AdminProjectReportRestController {

    private final ProjectReportRepository reportRepo;
    private final ProjectRepository projectRepo;
    private final NotificationRepository notificationRepo;
    private final ActivityService activityService;
    private final ProjectReportService service;

    public AdminProjectReportRestController(ProjectReportRepository reportRepo, ProjectRepository projectRepo,
                                            NotificationRepository notificationRepo, ActivityService activityService,
                                            ProjectReportService service) {
        this.reportRepo = reportRepo;
        this.projectRepo = projectRepo;
        this.notificationRepo = notificationRepo;
        this.activityService = activityService;
        this.service = service;
    }

    @GetMapping
    public List<ProjectReportDto> getAllReports() {
        return reportRepo.findAllWithReporterAndProject()
                .stream()
                .map(ProjectReportDto::new)
                .toList();
    }


    @PostMapping("/{id}/warn")
    public void warnOwner(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String message = body.get("message");
        ProjectReport report = reportRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        Project project = projectRepo.findById(report.getProject().getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        report.setStatus("reviewed");
        report.setActionTaken("Warning");
        report.setReviewedAt(Instant.now());
        reportRepo.save(report);

        Notification n = new Notification();
        n.setUser(project.getCreatedBy());
        n.setType("warning");
        n.setReferenceId(id);
        n.setContent("⚠️ Admin Warning: " + message);
        n.setStatus("unread");
        notificationRepo.save(n);

        activityService.logWithActor(1L, "ProjectReport", id, "warn",
                String.format("{\"project\":\"%s\",\"message\":\"%s\"}", project.getName(), message));
    }

    @PutMapping("/{id}/ban")
    public void removeProject(@PathVariable Long id) {
        ProjectReport report = reportRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        Project project = projectRepo.findById(report.getProject().getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        project.setStatus("removed");
        projectRepo.save(project);

        report.setStatus("reviewed");
        report.setActionTaken("Removed");
        report.setReviewedAt(Instant.now());
        reportRepo.save(report);

        Notification n = new Notification();
        n.setUser(project.getCreatedBy());
        n.setType("ban");
        n.setReferenceId(id);
        n.setContent("🚫 Your project \"" + project.getName() + "\" has been removed due to a violation.");
        n.setStatus("unread");
        notificationRepo.save(n);

        activityService.logWithActor(1L, "ProjectReport", id, "ban",
                String.format("{\"project\":\"%s\",\"status\":\"removed\"}", project.getName()));
    }
}
