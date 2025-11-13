package com.devcollab.controller.rest;

import com.devcollab.dto.request.ReportRequestDTO;
import com.devcollab.service.core.UserReportService;
import com.devcollab.service.core.ProjectReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller to handle user and project reporting actions.
 * Supports both user and project reports.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class UserReportRestController {

    private final UserReportService userReportService;
    private final ProjectReportService projectReportService; // 🔹 Add this line
    private String getEmailFromAuthentication(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauthToken) {
            var attributes = oauthToken.getPrincipal().getAttributes();
            return (String) attributes.get("email");
        }
        return auth.getName(); // Local login
    }
    /**
     * ✅ Create a new report (User or Project)
     */
    @PostMapping
    public ResponseEntity<?> createReport(
            @RequestBody ReportRequestDTO req,
            Authentication auth) {

        try {
            String reporterEmail = getEmailFromAuthentication(auth);

            // 🔹 USER report
            if ("user".equalsIgnoreCase(req.getType())) {
                userReportService.createUserReport(req, reporterEmail);
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "User report submitted successfully."
                ));
            }
            // 🔹 PROJECT report
            if ("project".equalsIgnoreCase(req.getType())) {
                projectReportService.createProjectReport(req, reporterEmail);
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Project report submitted successfully."
                ));
            }

            // 🔸 Invalid type
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Invalid report type: " + req.getType()
            ));

        } catch (IllegalArgumentException e) {
            // Handle validation issues (like self-reporting)
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to submit report: " + e.getMessage()
            ));
        }
    }
}
