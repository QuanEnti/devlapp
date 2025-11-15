package com.devcollab.dto.request;

import lombok.Data;

@Data
public class ProjectCreateRequestDTO {
    private String name;
    private String description;
    private String priority;   // thêm mới
    private String status;    // Active, Completed, In_Progress, On_Hold, Pending, Archived
    private String startDate;
    private String endDate;
    private String businessRule;
    private String coverImage;
}
