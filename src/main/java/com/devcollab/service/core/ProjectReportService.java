package com.devcollab.service.core;

import com.devcollab.domain.ProjectReport;
import com.devcollab.domain.User;
import com.devcollab.dto.ProjectReportDto;
import com.devcollab.dto.request.ReportRequestDTO;

import java.util.List;
import java.util.Map;

public interface ProjectReportService {
    void createProjectReport(ReportRequestDTO dto, String reporterEmail);
    Map<String, Object> getAllReports(int page, int size);
    ProjectReport updateReport(Long id, Map<String, String> body, User admin);
    void warnOwner(Long id, Map<String, String> body, User admin);
    void removeProject(Long id, User admin);
    ProjectReportDto getReportById(Long id);
    List<ProjectReportDto> getReportsByUser(String email);
    long countViolatedProjects();
}
