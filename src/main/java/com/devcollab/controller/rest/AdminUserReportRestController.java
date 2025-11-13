//package com.devcollab.controller.rest;
//
//import com.devcollab.domain.User;
//import com.devcollab.domain.UserReport;
//import com.devcollab.domain.Notification;
//import com.devcollab.dto.UserReportDto;
//import com.devcollab.repository.UserReportRepository;
//import com.devcollab.repository.UserRepository;
//import com.devcollab.repository.NotificationRepository;
//import com.devcollab.service.system.ActivityService;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Sort;
//
//import java.time.Instant;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/admin/reports")
//public class AdminUserReportRestController {
//
//    private final UserReportRepository reportRepo;
//    private final UserRepository userRepo;
//    private final NotificationRepository notificationRepo;
//    private final ActivityService activityService;
//
//    public AdminUserReportRestController(
//            UserReportRepository reportRepo,
//            UserRepository userRepo,
//            NotificationRepository notificationRepo,
//            ActivityService activityService
//    ) {
//        this.reportRepo = reportRepo;
//        this.userRepo = userRepo;
//        this.notificationRepo = notificationRepo;
//        this.activityService = activityService;
//    }
//
//    /** 🟢 Get all reports for admin view */
////    @GetMapping public List<UserReportDto> getAllReports() { return reportRepo.findAllWithUsers() .stream() .map(UserReportDto::new) .toList(); }
//
//    @GetMapping
//    public Map<String, Object> getAllReports(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size
//    ) {
//        if (page < 0) page = 0;
//        if (size <= 0 || size > 100) size = 10;
//
//        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
//        Page<UserReportDto> pageData = reportRepo.findAllWithUsers(pageable)
//                .map(UserReportDto::new);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("content", pageData.getContent());
//        response.put("currentPage", pageData.getNumber());
//        response.put("totalItems", pageData.getTotalElements());
//        response.put("totalPages", pageData.getTotalPages());
//        return response;
//    }
//
//
//    /** 🟡 Update report (status/action only) */
//    @PutMapping("/{id}")
//    public UserReport updateReport(@PathVariable Long id, @RequestBody Map<String, String> body) {
//        String status = body.get("status");
//        String actionTaken = body.get("actionTaken");
//
//        UserReport report = reportRepo.findById(id)
//                .orElseThrow(() -> new RuntimeException("Report not found"));
//
//        report.setStatus(status);
//        report.setActionTaken(actionTaken);
//        report.setReviewedAt(Instant.now());
//        reportRepo.save(report);
//
//        // 🧾 Log to Activity table
//        activityService.logWithActor(
//                null, // admin actor ID (you can replace with actual adminId if session supports it)
//                "UserReport",
//                id,
//                "update",
//                String.format("{\"status\":\"%s\",\"actionTaken\":\"%s\"}", status, actionTaken)
//        );
//
//        return report;
//    }
//
//    /** ⚠️ Admin sends warning to user */
//    @PostMapping("/{id}/warn")
//    public void warnUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
//        String message = body.get("message");
//
//        UserReport report = reportRepo.findById(id)
//                .orElseThrow(() -> new RuntimeException("Report not found"));
//
//        User reported = userRepo.findById(report.getReported().getUserId())
//                .orElseThrow(() -> new RuntimeException("Reported user not found"));
//
//        // Update report
//        report.setStatus("reviewed");
//        report.setActionTaken("Warning");
//        report.setReviewedAt(Instant.now());
//        reportRepo.save(report);
//
//        // Create notification
//        Notification n = new Notification();
//        n.setUser(reported);
//        n.setType("warning");
//        n.setReferenceId(id);
//        n.setContent("⚠️ Admin Warning: " + message);
//        n.setStatus("unread");
//        n.setCreatedAt(java.time.LocalDateTime.now());
//        notificationRepo.save(n);
//
//        // 🧾 Log action
//        activityService.logWithActor(
//                1L,
//                "UserReport",
//                id,
//                "warn",
//                String.format("{\"reportedUser\":\"%s\",\"message\":\"%s\"}", reported.getEmail(), message)
//        );
//    }
//
//    /** 🔴 Admin bans user */
//    @PutMapping("/{id}/ban")
//    public void banUser(@PathVariable Long id) {
//        UserReport report = reportRepo.findById(id)
//                .orElseThrow(() -> new RuntimeException("Report not found"));
//
//        User reported = userRepo.findById(report. getReported().getUserId())
//                .orElseThrow(() -> new RuntimeException("Reported user not found"));
//
//        // Update user status
//        reported.setStatus("banned");
//        userRepo.save(reported);
//
//        // Update report
//        report.setStatus("reviewed");
//        report.setActionTaken("Ban");
//        report.setReviewedAt(Instant.now());
//        reportRepo.save(report);
//
//        // Create notification
//        Notification n = new Notification();
//        n.setUser(reported);
//        n.setType("ban");
//        n.setReferenceId(id);
//        n.setContent("🚫 Your account has been banned due to violation of community guidelines.");
//        n.setStatus("unread");
//        n.setCreatedAt(java.time.LocalDateTime.now());
//        notificationRepo.save(n);
//
//        // 🧾 Log to Activity table
//        activityService.logWithActor(
//                1L,
//                "UserReport",
//                id,
//                "ban",
//                String.format("{\"reportedUser\":\"%s\",\"status\":\"banned\"}", reported.getEmail())
//        );
//    }
//}
package com.devcollab.controller.rest;

import com.devcollab.domain.User;
import com.devcollab.domain.UserReport;
import com.devcollab.service.core.UserReportService;
import com.devcollab.service.core.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminUserReportRestController {

    private final UserReportService userReportService;
    private final UserService userService;
    /** 🟢 Get all reports (paginated) */
    @GetMapping
    public Map<String, Object> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return userReportService.getAllReports(page, size);
    }
    private String getEmailFromAuthentication(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauthToken) {
            var attributes = oauthToken.getPrincipal().getAttributes();
            return (String) attributes.get("email");
        }
        return auth.getName(); // Local login
    }
    /** 🟡 Update report */
    @PutMapping("/{id}")
    public UserReport updateReport(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        String email =  getEmailFromAuthentication(auth);
        User current = userService.getByEmail(email).orElse(null);
        return userReportService.updateReport(id, body,current);
    }

    /** ⚠️ Warn user */
    @PostMapping("/{id}/warn")
    public void warnUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userReportService.warnUser(id, body);
    }

    /** 🔴 Ban user */
    @PutMapping("/{id}/ban")
    public void banUser(@PathVariable Long id,Authentication auth) {
        String email =  getEmailFromAuthentication(auth);
        User current = userService.getByEmail(email).orElse(null);
        userReportService.banUser(id,current);
    }
}

