package com.devcollab.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskDatesUpdateReq {
    private String startDate; 
    private String dueDate;
    private String recurring; 
    private String reminder; 
    

}
