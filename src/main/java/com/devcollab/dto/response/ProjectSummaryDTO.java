package com.devcollab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSummaryDTO {
    private long total;
    private long active;
    private long completed;
    private long onHold;
    private long pending;
    private long inProgress;
}
