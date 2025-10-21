package com.devcollab.repository.userprofileRepo;

import com.devcollab.domain.profileEnity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
public interface UserProfileRepository extends JpaRepository<User,Long> {
    // Tải user cùng với projectMemberships và tasks
    @EntityGraph(attributePaths = {"projectMemberships.project", "assignedTasks"})
    Optional<User> findByUserId(Long id);

    // Lấy danh sách người cùng làm việc với user (trừ chính họ)
    @Query("""
        SELECT DISTINCT pm2.user 
        FROM ProjectMember pm1 
        JOIN ProjectMember pm2 ON pm1.project.projectId = pm2.project.projectId 
        WHERE pm1.user.userId = :userId 
          AND pm2.user.userId <> :userId
    """)
    List<User> findCollaboratorsByUserId(@Param("userId") Long userId);
}

