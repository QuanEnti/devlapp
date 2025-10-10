package com.devcollab.repository;

import com.devcollab.domain.ProjectMember;
import com.devcollab.domain.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    List<ProjectMember> findByProject_ProjectId(Long projectId);

    List<ProjectMember> findByUser_UserId(Long userId);
}
