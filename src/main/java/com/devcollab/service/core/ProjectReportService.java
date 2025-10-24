package com.devcollab.service.core;

import com.devcollab.dto.ProjectReportDto;
import com.devcollab.domain.ProjectReport;

import java.util.List;

public interface ProjectReportService {
    List<ProjectReportDto> getAll();
    ProjectReportDto create(ProjectReport report);
}
