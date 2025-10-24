package com.devcollab.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.devcollab.domain.Task;
import lombok.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {
    private Long projectId;
    private String name;
    private String description;
    private String coverImage;
    private String status;
    private LocalDate dueDate;
    private LocalDateTime updatedAt;
    private List<String> memberAvatars;
    private List<String> memberNames;
    private int memberCount;

    public ProjectDTO(Long projectId, String name, String coverImage, String status) {
        this.projectId = projectId;
        this.name = name;
        this.coverImage = coverImage;
        this.status = status;
    }   

    public ProjectDTO(Long projectId, String name, String description, String coverImage, String status,
            LocalDate dueDate, LocalDateTime updatedAt) {
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.coverImage = coverImage;
        this.status = status;
        this.dueDate = dueDate;
        this.updatedAt = updatedAt;
    }
}
