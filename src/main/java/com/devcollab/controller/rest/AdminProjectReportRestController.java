//package com.devcollab.controller.rest;
//
//import com.devcollab.domain.ProjectReport;
//import com.devcollab.domain.Project;
//import com.devcollab.domain.Notification;
//import com.devcollab.dto.ProjectReportDto;
//import com.devcollab.repository.ProjectReportRepository;
//import com.devcollab.repository.ProjectRepository;
//import com.devcollab.repository.NotificationRepository;
//import com.devcollab.service.system.ActivityService;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.Instant;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/api/admin/project-reports")
//public class AdminProjectReportRestController {
//
//    private final ProjectReportRepository reportRepo;
//    private final ProjectRepository projectRepo;
//    private final NotificationRepository notificationRepo;
//    private final ActivityService activityService;
//
//    public AdminProjectReportRestController(ProjectReportRepository reportRepo, ProjectRepository projectRepo,
//                                            NotificationRepository notificationRepo, ActivityService activityService) {
//        this.reportRepo = reportRepo;
//        this.projectRepo = projectRepo;
//        this.notificationRepo = notificationRepo;
//        this.activityService = activityService;
//    }
//
//    @GetMapping
//    public Map<String, Object> getAllReports(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//
//        // Validate parameters
//        if (page < 0) page = 0;
//        if (size <= 0 || size > 100) size = 10;
//
//        Pageable pageable = PageRequest.of(page, size);
//
//        // Use the EntityGraph method
//        Page<ProjectReport> projectReportsPage = reportRepo.findAllByOrderByCreatedAtDesc(pageable);
//
//        // Convert to DTOs
//        List<ProjectReportDto> content = projectReportsPage.getContent()
//                .stream()
//                .map(ProjectReportDto::new)
//                .collect(Collectors.toList());
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("content", content);
//        response.put("currentPage", projectReportsPage.getNumber());
//        response.put("totalItems", projectReportsPage.getTotalElements());
//        response.put("totalPages", projectReportsPage.getTotalPages());
//        return response;
//    }
//
//    @PostMapping("/{id}/warn")
//    public void warnOwner(@PathVariable Long id, @RequestBody Map<String, String> body) {
//        String message = body.get("message");
//        ProjectReport report = reportRepo.findById(id)
//                .orElseThrow(() -> new RuntimeException("Report not found"));
//        Project project = projectRepo.findById(report.getProject().getProjectId())
//                .orElseThrow(() -> new RuntimeException("Project not found"));
//
//        report.setStatus("reviewed");
//        report.setActionTaken("Warning");
//        report.setReviewedAt(Instant.now());
//        reportRepo.save(report);
//
//        Notification n = new Notification();
//        n.setUser(project.getCreatedBy());
//        n.setType("warning");
//        n.setReferenceId(id);
//        n.setContent("⚠️ Admin Warning: " + message);
//        n.setStatus("unread");
//        notificationRepo.save(n);
//
//        activityService.logWithActor(1L, "ProjectReport", id, "warn",
//                String.format("{\"project\":\"%s\",\"message\":\"%s\"}", project.getName(), message));
//    }
//
//    @PutMapping("/{id}/ban")
//    public void removeProject(@PathVariable Long id) {
//        ProjectReport report = reportRepo.findById(id)
//                .orElseThrow(() -> new RuntimeException("Report not found"));
//        Project project = projectRepo.findById(report.getProject().getProjectId())
//                .orElseThrow(() -> new RuntimeException("Project not found"));
//
//        project.setStatus("removed");
//        projectRepo.save(project);
//
//        report.setStatus("reviewed");
//        report.setActionTaken("Removed");
//        report.setReviewedAt(Instant.now());
//        reportRepo.save(report);
//
//        Notification n = new Notification();
//        n.setUser(project.getCreatedBy());
//        n.setType("ban");
//        n.setReferenceId(id);
//        n.setContent("🚫 Your project \"" + project.getName() + "\" has been removed due to a violation.");
//        n.setStatus("unread");
//        notificationRepo.save(n);
//
//        activityService.logWithActor(1L, "ProjectReport", id, "ban",
//                String.format("{\"project\":\"%s\",\"status\":\"removed\"}", project.getName()));
//    }
//}
package com.devcollab.controller.rest;

import com.devcollab.domain.User;
import com.devcollab.service.core.ProjectReportService;
import com.devcollab.service.core.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/project-reports")
@RequiredArgsConstructor
public class AdminProjectReportRestController {

    private final ProjectReportService projectReportService;

    private final UserService userService;

    /** 🟢 Lấy tất cả project reports (paginated) */
    @GetMapping
    public Map<String, Object> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return projectReportService.getAllReports(page, size);
    }
    private String getEmailFromAuthentication(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauthToken) {
            var attributes = oauthToken.getPrincipal().getAttributes();
            return (String) attributes.get("email");
        }
        return auth.getName(); // Local login
    }

    /** ⚠️ Gửi cảnh báo cho chủ dự án */
    @PostMapping("/{id}/warn")
    public void warnOwner(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        String email = getEmailFromAuthentication(auth);
        User user = userService.getByEmail(email).orElse(null);
        projectReportService.warnOwner(id, body,user);
    }

    /** 🔴 Xóa / Ban dự án */
    @PutMapping("/{id}/ban")
    public void removeProject(@PathVariable Long id,Authentication auth)
    {
        String email = getEmailFromAuthentication(auth);
        User user = userService.getByEmail(email).orElse(null);
        projectReportService.removeProject(id,user);
    }
}
