package com.devcollab.repository.taskRepository;

import com.devcollab.domain.profileEnity.ProjectMember;
import com.devcollab.domain.profileEnity.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    // Spring Data JPA đủ thông minh để tạo method này
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
}
