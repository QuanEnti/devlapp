package com.devcollab.dto.request;

import lombok.Data;

@Data
public class MoveTaskRequest {
    private Long targetColumnId;
    private int newOrderIndex;
}
