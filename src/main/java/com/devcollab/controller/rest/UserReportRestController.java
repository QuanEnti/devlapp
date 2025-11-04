package com.devcollab.controller.rest;

import com.devcollab.dto.request.ReportRequestDTO;
import com.devcollab.service.core.UserReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller to handle user and project reporting actions.
 * Future-proofed for multiple report types.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class UserReportRestController {

    private final UserReportService userReportService;

    /**
     * ✅ Create a new report (User or Project in the future)
     */
    @PostMapping
    public ResponseEntity<?> createReport(
            @RequestBody ReportRequestDTO req,
            Authentication auth) {

        try {
            String reporterEmail = auth != null ? auth.getName() : "anonymous";

            // Handle USER reports for now
            if ("user".equalsIgnoreCase(req.getType())) {
                userReportService.createUserReport(req, reporterEmail);
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "User report submitted successfully."
                ));
            }

            // 🔹 Future: handle project reports
            if ("project".equalsIgnoreCase(req.getType())) {
                // placeholder for future project reporting logic
                return ResponseEntity.ok(Map.of(
                        "status", "pending",
                        "message", "Project report functionality coming soon!"
                ));
            }

            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Invalid report type: " + req.getType()
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
