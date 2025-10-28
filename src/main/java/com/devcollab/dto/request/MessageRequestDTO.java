package com.devcollab.dto.request;

import lombok.Data;

@Data
public class MessageRequestDTO {
    private Long projectId;
    private String content;
}
