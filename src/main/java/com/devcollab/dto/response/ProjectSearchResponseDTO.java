package com.devcollab.dto.response;

import com.devcollab.domain.Project;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSearchResponseDTO {

    private Long projectId;
    private String name;
    private String description;
    private String priority;
    private String status;
    private LocalDate startDate;
    private LocalDate dueDate;
    private String createdByEmail;

    // Constructor chuyển từ Entity sang DTO
    public ProjectSearchResponseDTO(Project project) {
        this.projectId = project.getProjectId();
        this.name = project.getName();
        this.description = project.getDescription();
        this.priority = project.getPriority();
        this.status = project.getStatus();
        this.startDate = project.getStartDate();
        this.dueDate = project.getDueDate();
        this.createdByEmail = project.getCreatedBy() != null
                ? project.getCreatedBy().getEmail()
                : null;
    }
}
