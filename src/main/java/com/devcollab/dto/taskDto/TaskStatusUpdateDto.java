package com.devcollab.dto.taskDto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

// DTO nhận request khi user đổi status
@Data
public class TaskStatusUpdateDto {
    // Ràng buộc status phải là 1 trong 4 giá trị từ DB
    @Pattern(regexp = "^(To Do|In Progress|Review|Done)$", message = "Invalid status value")
    @NotEmpty
    private String status;
}
