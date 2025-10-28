package com.devcollab.dto;

import com.devcollab.domain.UserReport;
import java.time.Instant;

public class UserReportDto {
    private Long id;
    private String reporterName;
    private String reportedName;
    private String reason;
    private String details;
    private String status;
    private String actionTaken;
    private Instant createdAt;
    private Instant reviewedAt;
    private String pathUrl;

    public UserReportDto(UserReport r) {
        this.id = r.getId();
        this.reporterName = r.getReporter() != null ? r.getReporter().getName() : null;
        this.reportedName = r.getReported() != null ? r.getReported().getName() : null;
        this.reason = r.getReason();
        this.details = r.getDetails();
        this.status = r.getStatus();
        this.actionTaken = r.getActionTaken();
        this.createdAt = r.getCreatedAt();
        this.reviewedAt = r.getReviewedAt();
        this.pathUrl = r.getProofUrl();
    }

    // getters

    public Instant getReviewedAt() {return reviewedAt;}
    public Long getId() { return id; }
    public String getReporterName() { return reporterName; }
    public String getReportedName() { return reportedName; }
    public String getReason() { return reason; }
    public String getDetails() { return details; }
    public String getStatus() { return status; }
    public String getActionTaken() { return actionTaken; }
    public Instant getCreatedAt() { return createdAt; }
    public String getPathUrl() { return pathUrl; }
}
