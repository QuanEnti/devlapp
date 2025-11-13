package com.devcollab.service.impl.core;

import com.devcollab.domain.Notification;
import com.devcollab.domain.User;
import com.devcollab.domain.UserReport;
import com.devcollab.dto.UserReportDto;
import com.devcollab.dto.request.ReportRequestDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.NotificationRepository;
import com.devcollab.repository.UserReportRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.core.UserReportService;
import com.devcollab.service.system.ActivityService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class UserReportServiceImpl implements UserReportService {

    private final UserReportRepository reportRepo;
    private final UserRepository userRepo;
    private final NotificationRepository notificationRepo;
    private final ActivityService activityService;

    @Override
    public void createUserReport(ReportRequestDTO dto, String reporterEmail) {
        User reporter = userRepo.findByEmail(reporterEmail)
                .orElseThrow(() -> new NotFoundException("Reporter not found"));

        User reported = userRepo.findById(dto.getReportedId())
                .orElseThrow(() -> new NotFoundException("Reported user not found"));

        UserReport report = new UserReport();
        report.setReporter(reporter);
        report.setReported(reported);
        report.setReason(dto.getReason());
        report.setDetails(dto.getDetails());
        report.setProofUrl(dto.getProofUrl());
        report.setStatus("pending");
        report.setCreatedAt(Instant.now());

        reportRepo.save(report);
    }

    /** 🟢 Lấy danh sách report có phân trang */
    @Override
    public Map<String, Object> getAllReports(int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 10;

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserReportDto> pageData = reportRepo.findAllWithUsers(pageable)
                .map(UserReportDto::new);

        Map<String, Object> response = new HashMap<>();
        response.put("content", pageData.getContent());
        response.put("currentPage", pageData.getNumber());
        response.put("totalItems", pageData.getTotalElements());
        response.put("totalPages", pageData.getTotalPages());
        return response;
    }

    /** 🟡 Cập nhật report */
    @Override
    public UserReport updateReport(Long id, Map<String, String> body,User admin) {
        String status = body.get("status");
        String actionTaken = body.get("actionTaken");

        UserReport report = reportRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setStatus(status);
        report.setActionTaken(actionTaken);
        report.setReviewedAt(Instant.now());
        reportRepo.save(report);

         activityService.logWithActor(
                 admin.getUserId(), // admin ID (hoặc thay bằng ID người đang đăng nhập)
                 "UserReport",
                 id,
                 "update",
                 String.format("{\"status\":\"%s\",\"actionTaken\":\"%s\"}", status, actionTaken)
         );

        return report;
    }

    /** ⚠️ Gửi cảnh báo cho user */
    @Override
    public void warnUser(Long id, Map<String, String> body) {
        String message = body.get("message");

        UserReport report = reportRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        User reported = userRepo.findById(report.getReported().getUserId())
                .orElseThrow(() -> new RuntimeException("Reported user not found"));

        // Cập nhật trạng thái
        report.setStatus("reviewed");
        report.setActionTaken("Warning");
        report.setReviewedAt(Instant.now());
        reportRepo.save(report);

        // // Gửi notification
         Notification n = new Notification();
         n.setUser(reported);
         n.setType("WARNING");
         n.setReferenceId(id);
         n.setMessage("⚠️ Admin Warning: " + message);
         n.setStatus("unread");
         n.setCreatedAt(java.time.LocalDateTime.now());
         n.setLink("view/user-report/"+ id);
         notificationRepo.save(n);

        // // Ghi log hoạt động
         activityService.logWithActor(reported.getUserId(),
                 "UserReport",
                 id,
                 "warn",
                 String.format("{\"reportedUser\":\"%s\",\"message\":\"%s\"}",
                         reported.getEmail(), message)
         );
    }

    /** 🔴 Ban user */
    @Override
    public void banUser(Long id,User admin) {
        UserReport report = reportRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        User reported = userRepo.findById(report.getReported().getUserId())
                .orElseThrow(() -> new RuntimeException("Reported user not found"));

        // Ban user
        reported.setStatus("banned");
        userRepo.save(reported);

        // Cập nhật report
        report.setStatus("reviewed");
        report.setActionTaken("Ban");
        report.setReviewedAt(Instant.now());
        reportRepo.save(report);

        // // Notification
         Notification n = new Notification();
         n.setUser(reported);
         n.setType("BAN");
         n.setReferenceId(id);
         n.setMessage("🚫 Your account has been banned due to violation of community guidelines.");
         n.setStatus("unread");
         n.setCreatedAt(java.time.LocalDateTime.now());
        n.setLink("view/user-report/"+ id);
         notificationRepo.save(n);

        // // Log
         activityService.logWithActor(
                 admin.getUserId(),
                 "UserReport",
                 id,
                 "ban",
                 String.format("{\"reportedUser\":\"%s\",\"status\":\"banned\"}",
                         reported.getEmail())
         );
    }
    @Override
    public UserReportDto getReportById(Long id) {
        UserReport report = reportRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Report not found with id: " + id));

        return new UserReportDto(report);
    }

    @Override
    public List<UserReportDto> getReportsByUser(String email) {
        // optional - existing logic if needed
        return reportRepo.findAll().stream()
                .map(UserReportDto::new)
                .toList();
    }
}

