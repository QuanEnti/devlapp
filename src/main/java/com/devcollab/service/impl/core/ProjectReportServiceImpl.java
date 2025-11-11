package com.devcollab.service.impl.core;

import com.devcollab.domain.Notification;
import com.devcollab.domain.Project;
import com.devcollab.domain.ProjectReport;
import com.devcollab.dto.ProjectReportDto;
import com.devcollab.repository.NotificationRepository;
import com.devcollab.repository.ProjectReportRepository;
import com.devcollab.repository.ProjectRepository;
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
        private final NotificationRepository notificationRepo;
        private final ActivityService activityService;

        @Override
        public Map<String, Object> getAllReports(int page, int size) {
                if (page < 0)
                        page = 0;
                if (size <= 0 || size > 100)
                        size = 10;

                Pageable pageable = PageRequest.of(page, size);
                Page<ProjectReport> reportPage = reportRepo.findAllByOrderByCreatedAtDesc(pageable);

                List<ProjectReportDto> content = reportPage.getContent().stream()
                                .map(ProjectReportDto::new).collect(Collectors.toList());

                Map<String, Object> response = new HashMap<>();
                response.put("content", content);
                response.put("currentPage", reportPage.getNumber());
                response.put("totalItems", reportPage.getTotalElements());
                response.put("totalPages", reportPage.getTotalPages());
                return response;
        }

        /** ⚠️ Gửi cảnh báo (Warning) cho chủ dự án */
        @Override
        public void warnOwner(Long id, Map<String, String> body) {
                String message = body.get("message");

                ProjectReport report = reportRepo.findById(id)
                                .orElseThrow(() -> new RuntimeException("Report not found"));
                Project project = projectRepo.findById(report.getProject().getProjectId())
                                .orElseThrow(() -> new RuntimeException("Project not found"));

                // Update report
                report.setStatus("reviewed");
                report.setActionTaken("Warning");
                report.setReviewedAt(Instant.now());
                reportRepo.save(report);

                // Send notification
                Notification n = new Notification();
                n.setUser(project.getCreatedBy());
                n.setType("warning");
                n.setReferenceId(id);
                // n.setContent("⚠️ Admin Warning: " + message);
                n.setStatus("unread");
                n.setCreatedAt(java.time.LocalDateTime.now());
                notificationRepo.save(n);

                // // Log admin action
                // activityService.logWithActor(
                // 1L, // admin
                // "ProjectReport",
                // id,
                // "warn",
                // String.format("{\"project\":\"%s\",\"message\":\"%s\"}", project.getName(),
                // message)
                // );
        }

        /** 🔴 Xóa hoặc ban dự án */
        @Override
        public void removeProject(Long id) {
                ProjectReport report = reportRepo.findById(id)
                                .orElseThrow(() -> new RuntimeException("Report not found"));
                Project project = projectRepo.findById(report.getProject().getProjectId())
                                .orElseThrow(() -> new RuntimeException("Project not found"));

                // Update project status
                project.setStatus("removed");
                projectRepo.save(project);

                // Update report
                report.setStatus("reviewed");
                report.setActionTaken("Removed");
                report.setReviewedAt(Instant.now());
                reportRepo.save(report);

                // // Send notification
                // Notification n = new Notification();
                // n.setUser(project.getCreatedBy());
                // n.setType("ban");
                // n.setReferenceId(id);
                // n.setContent("🚫 Your project \"" + project.getName() + "\" has been removed due
                // to a violation.");
                // n.setStatus("unread");
                // n.setCreatedAt(java.time.LocalDateTime.now());
                // notificationRepo.save(n);

                // Log admin action
                activityService.logWithActor(1L, "ProjectReport", id, "ban", String.format(
                                "{\"project\":\"%s\",\"status\":\"removed\"}", project.getName()));
        }
}

