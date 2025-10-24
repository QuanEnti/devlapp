package com.devcollab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ProjectPerformanceDTO {
    private List<String> labels;
    private List<Long> achieved; 
    private List<Long> target; 
}
