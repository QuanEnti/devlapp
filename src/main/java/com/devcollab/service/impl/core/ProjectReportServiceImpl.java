package com.devcollab.service.impl.core;

import com.devcollab.domain.ProjectReport;
import com.devcollab.dto.ProjectReportDto;
import com.devcollab.repository.ProjectReportRepository;
import com.devcollab.service.core.ProjectReportService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service  // ✅ this annotation makes it a Spring bean
public class ProjectReportServiceImpl implements ProjectReportService {

    private final ProjectReportRepository repo;

    public ProjectReportServiceImpl(ProjectReportRepository repo) {
        this.repo = repo;
    }
    @Override
    public List<ProjectReportDto> getAll() {
        return repo.findAllWithRelations().stream().map(this::toDto).toList();
    }

    @Override
    public ProjectReportDto create(ProjectReport report) {
        return toDto(repo.save(report));
    }

    private ProjectReportDto toDto(ProjectReport r) {
        ProjectReportDto dto = new ProjectReportDto();
        dto.setReportId(r.getId());
        dto.setReporterId(r.getReporter().getUserId());
        dto.setReporterName(r.getReporter().getName());
        dto.setProjectId(r.getProject().getProjectId());
        dto.setProjectName(r.getProject().getName());
        dto.setReason(r.getReason());
        dto.setDetails(r.getDetails());
        dto.setProofUrl(r.getProofUrl());
        dto.setStatus(r.getStatus());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setReviewedAt(r.getReviewedAt());
        dto.setActionTaken(r.getActionTaken());
        return dto;
    }
}
