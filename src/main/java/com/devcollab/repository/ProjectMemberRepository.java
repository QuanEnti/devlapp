package com.devcollab.repository;

import com.devcollab.domain.Project;
import com.devcollab.domain.ProjectMember;
import com.devcollab.domain.ProjectMemberId;
import com.devcollab.dto.UserDto;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    List<ProjectMember> findByProject_ProjectId(Long projectId);

    List<ProjectMember> findByUser_UserId(Long userId);

    @Query("""
        SELECT new com.devcollab.dto.UserDto(u)
        FROM ProjectMember pm
        JOIN pm.user u
        WHERE pm.project.projectId = :projectId
    """)
    List<UserDto> findMembersByProjectId(@Param("projectId") Long projectId);

    // 🔹 Find all projects a user is a member of
    @Query("""
        SELECT pm.project
        FROM ProjectMember pm
        WHERE pm.user.userId = :userId
    """)
    List<Project> findProjectsByUserId(@Param("userId") Long userId);

    // 🔹 Check if a user already belongs to a project
    boolean existsByProject_ProjectIdAndUser_UserId(Long projectId, Long userId);

    // 🔹 Add a new project member
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO dbo.ProjectMember (project_id, user_id, role_in_project, joined_at) VALUES (:projectId, :userId, :role, GETDATE())", nativeQuery = true)
    void addMember(@Param("projectId") Long projectId,
                   @Param("userId") Long userId,
                   @Param("role") String role);
}

