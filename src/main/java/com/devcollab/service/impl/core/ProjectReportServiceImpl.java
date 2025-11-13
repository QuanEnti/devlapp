package com.devcollab.service.impl.core;

import com.devcollab.domain.Notification;
import com.devcollab.domain.Project;
import com.devcollab.domain.ProjectReport;
import com.devcollab.domain.User;
import com.devcollab.dto.ProjectReportDto;
import com.devcollab.dto.request.ReportRequestDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.NotificationRepository;
import com.devcollab.repository.ProjectReportRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.core.ProjectReportService;
import com.devcollab.service.system.ActivityService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectReportServiceImpl implements ProjectReportService {

        private final ProjectReportRepository reportRepo;
        private final ProjectRepository projectRepo;
        private final UserRepository userRepo;
        private final NotificationRepository notificationRepo;
        private final ActivityService activityService;

        /** ✅ Create new project report */
        @Override
        public void createProjectReport(ReportRequestDTO dto, String reporterEmail) {
                User reporter = userRepo.findByEmail(reporterEmail)
                                .orElseThrow(() -> new NotFoundException("Reporter not found"));

                Project project = projectRepo.findById(dto.getReportedId())
                                .orElseThrow(() -> new NotFoundException("Project not found"));

                // Prevent self-report
                if (project.getCreatedBy().getUserId().equals(reporter.getUserId())) {
                        throw new IllegalArgumentException("You cannot report your own project.");
                }

                ProjectReport report = new ProjectReport();
                report.setReporter(reporter);
                report.setProject(project);
                report.setReason(dto.getReason());
                report.setDetails(dto.getDetails());
                report.setProofUrl(dto.getProofUrl());
                report.setStatus("pending");
                report.setCreatedAt(Instant.now());

                reportRepo.save(report);
        }

        /** 🟢 Get all project reports (paginated) */
        @Override
        public Map<String, Object> getAllReports(int page, int size) {
                if (page < 0)
                        page = 0;
                if (size <= 0 || size > 100)
                        size = 10;

                Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                Page<ProjectReport> reportPage = reportRepo.findAll(pageable);

                List<ProjectReportDto> content = reportPage.getContent().stream()
                                .map(ProjectReportDto::new).collect(Collectors.toList());

                Map<String, Object> response = new HashMap<>();
                response.put("content", content);
                response.put("currentPage", reportPage.getNumber());
                response.put("totalItems", reportPage.getTotalElements());
                response.put("totalPages", reportPage.getTotalPages());
                return response;
        }

        /** 🟡 Update project report (status, actionTaken) */
        @Override
        public ProjectReport updateReport(Long id, Map<String, String> body, User admin) {
                String status = body.get("status");
                String actionTaken = body.get("actionTaken");

                ProjectReport report = reportRepo.findById(id).orElseThrow(
                                () -> new RuntimeException("Project report not found"));

                report.setStatus(status);
                report.setActionTaken(actionTaken);
                report.setReviewedAt(Instant.now());
                reportRepo.save(report);

                activityService.logWithActor(admin.getUserId(), "ProjectReport", id, "update",
                                String.format("{\"status\":\"%s\",\"actionTaken\":\"%s\"}", status,
                                                actionTaken));

                return report;
        }

        /** ⚠️ Send warning notification to project owner */
        @Override
        public void warnOwner(Long id, Map<String, String> body, User admin) {
                String message = body.get("message");

                ProjectReport report = reportRepo.findById(id)
                                .orElseThrow(() -> new RuntimeException("Report not found"));

                Project project = projectRepo.findById(report.getProject().getProjectId())
                                .orElseThrow(() -> new RuntimeException("Project not found"));

                User owner = project.getCreatedBy();

                // Update report
                report.setStatus("reviewed");
                report.setActionTaken("Warning");
                report.setReviewedAt(Instant.now());
                reportRepo.save(report);

                // Send notification
                Notification n = new Notification();
                n.setUser(owner);
                n.setType("WARNING");
                n.setReferenceId(id);
                n.setMessage("⚠️ Your project " + project.getName() + " receive a warning: "
                                + message);
                n.setStatus("unread");
                n.setCreatedAt(java.time.LocalDateTime.now());
                n.setLink("/view/project-report/" + id);
                notificationRepo.save(n);

                // Log action
                activityService.logWithActor(admin.getUserId(), "ProjectReport", id, "warn",
                                String.format("{\"project\":\"%s\",\"message\":\"%s\"}",
                                                project.getName(), message));
        }

        /** 🔴 Remove or ban a project */
        @Override
        public void removeProject(Long id, User admin) {
                ProjectReport report = reportRepo.findById(id)
                                .orElseThrow(() -> new RuntimeException("Report not found"));

                Project project = projectRepo.findById(report.getProject().getProjectId())
                                .orElseThrow(() -> new RuntimeException("Project not found"));

                // Mark project as removed/archived
                project.setStatus("Archived");
                projectRepo.save(project);

                // Update report info
                report.setStatus("reviewed");
                report.setActionTaken("Removed");
                report.setReviewedAt(Instant.now());
                reportRepo.save(report);

                // Send notification
                Notification n = new Notification();
                n.setUser(project.getCreatedBy());
                n.setType("BAN");
                n.setReferenceId(id);
                n.setMessage("🚫 Your project \"" + project.getName()
                                + "\" has been removed due to violations.");
                n.setStatus("unread");
                n.setCreatedAt(java.time.LocalDateTime.now());
                n.setLink("/view/project-report/" + id);
                notificationRepo.save(n);

                // Log admin action
                activityService.logWithActor(admin.getUserId(), "ProjectReport", id, "ban",
                                String.format("{\"project\":\"%s\",\"status\":\"removed\"}",
                                                project.getName()));
        }

        /** 🔍 Get single project report by ID */
        @Override
        public ProjectReportDto getReportById(Long id) {
                ProjectReport report =
                                reportRepo.findById(id).orElseThrow(() -> new NotFoundException(
                                                "Project report not found with id: " + id));

                return new ProjectReportDto(report);
        }

        /** 📋 Optional: Get all reports submitted by specific user */
        @Override
        public List<ProjectReportDto> getReportsByUser(String email) {
                return reportRepo.findAll().stream().map(ProjectReportDto::new)
                                .collect(Collectors.toList());
        }

        @Override
        public long countViolatedProjects() {
                long pending = reportRepo.countByStatus("pending");
                long reviewed = reportRepo.countByStatus("reviewed");
                return pending + reviewed;
        }
}
