package com.devcollab.dto;

import com.devcollab.domain.ProjectReport;

import java.time.Instant;

public class ProjectReportDto {
    private Long reportId;
    private Long reporterId;
    private String reporterName;
    private Long projectId;
    private String projectName;
    private String reason;
    private String details;
    private String proofUrl;
    private String status;
    private Instant createdAt;
    private Instant reviewedAt;
    private String actionTaken;

    public ProjectReportDto() {}

    // 🔹 convenient constructor for quick mapping
    public ProjectReportDto(ProjectReport r) {
        this.reportId = r.getId();
        if (r.getReporter() != null) {
            this.reporterId = r.getReporter().getUserId();
            this.reporterName = r.getReporter().getName();
        }
        if (r.getProject() != null) {
            this.projectId = r.getProject().getProjectId();
            this.projectName = r.getProject().getName();
        }
        this.reason = r.getReason();
        this.details = r.getDetails();
        this.proofUrl = r.getProofUrl();
        this.status = r.getStatus();
        this.createdAt = r.getCreatedAt();
        this.reviewedAt = r.getReviewedAt();
        this.actionTaken = r.getActionTaken();
    }

    public ProjectReportDto(ProjectReportDto projectReportDto) {
    }

    // --- Getters and Setters ---
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getProofUrl() { return proofUrl; }
    public void setProofUrl(String proofUrl) { this.proofUrl = proofUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
}
