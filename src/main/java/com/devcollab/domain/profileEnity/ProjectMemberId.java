package com.devcollab.domain.profileEnity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.SecureRandomParameters;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ProjectMemberId  implements SecureRandomParameters {
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "user_id")
    private Long userId;
}
