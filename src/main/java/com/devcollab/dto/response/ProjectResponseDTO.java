package com.devcollab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectResponseDTO {
    private Long projectId;
    private String name;
    private String description;
    private String businessRule;
    private String priority;
    private String status;
    private String visibility;
    private LocalDate startDate;
    private LocalDate dueDate;
    private String createdByEmail;
    private String coverImage;
}