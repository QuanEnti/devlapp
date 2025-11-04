package com.devcollab.dto.request;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequestDTO {
    private String type;       // "user" | "project"
    private Long reportedId;   // user_id or project_id depending on type
    private String reason;
    private String details;
    private String proofUrl;
}

