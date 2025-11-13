package com.devcollab.dto.request;

import lombok.Data;

@Data
public class ProjectCreateRequestDTO {
    private String name;
    private String description;
    private String priority;   // thêm mới
    private String startDate;
    private String endDate;
    private String businessRule;
}
