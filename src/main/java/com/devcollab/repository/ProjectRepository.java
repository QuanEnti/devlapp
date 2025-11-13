package com.devcollab.repository;

import com.devcollab.domain.Project;
import com.devcollab.dto.ProjectDTO;

import com.devcollab.dto.userTaskDto.ProjectFilterDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByCreatedBy_UserId(Long userId);

    Optional<Project> findByInviteLink(String inviteLink);

    long countByStatus(String status);

    List<Project> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name,
            String description);

    @Query("""
                SELECT DISTINCT p
                FROM Project p
                LEFT JOIN FETCH p.members
                WHERE p.projectId = :id
            """)
    Optional<Project> findByIdWithMembers(@Param("id") Long id);

    @Query("""
                SELECT p.status, COUNT(p)
                FROM Project p
                GROUP BY p.status
                ORDER BY p.status
            """)
    List<Object[]> countProjectsByStatus();

    @Query(value = """
            SELECT
                FORMAT(p.updated_at, 'MMMM') AS month_name,
                COUNT(*) AS total
            FROM project p
            WHERE
                UPPER(p.status) = 'COMPLETED'
                AND p.updated_at IS NOT NULL
                AND p.updated_at >= DATEADD(MONTH, -6, GETDATE())
            GROUP BY FORMAT(p.updated_at, 'MMMM'), MONTH(p.updated_at)
            ORDER BY MONTH(p.updated_at)
            """, nativeQuery = true)
    List<Object[]> countCompletedProjectsPerMonth();

    @Query(value = """
            SELECT
                FORMAT(p.updated_at, 'MMMM') AS month_name,
                COUNT(*) AS total
            FROM project p
            WHERE
                UPPER(p.status) = 'COMPLETED'
                AND p.updated_at IS NOT NULL
                AND p.updated_at >= :startDate
            GROUP BY FORMAT(p.updated_at, 'MMMM'), MONTH(p.updated_at)
            ORDER BY MONTH(p.updated_at)
            """, nativeQuery = true)
    List<Object[]> countCompletedProjectsSince(@Param("startDate") LocalDateTime startDate);

    @Query(value = """
                SELECT
                    p.status, COUNT(*)
                FROM project p
                WHERE p.updated_at >= :startDate
                GROUP BY p.status
                ORDER BY p.status
            """, nativeQuery = true)
    List<Object[]> countProjectsByStatusSince(@Param("startDate") LocalDateTime startDate);

    @Query("""
                SELECT new com.devcollab.dto.ProjectDTO(
                    p.projectId,
                    p.name,
                    p.coverImage,
                    p.status
                )
                FROM Project p
                WHERE p.status IS NOT NULL
                  AND p.status <> 'Archived'
                ORDER BY p.updatedAt DESC
            """)
    List<ProjectDTO> findTop9Projects(Pageable pageable);

    @Query("""
                SELECT new com.devcollab.dto.ProjectDTO(
                    p.projectId,
                    p.name,
                    p.description,
                    p.coverImage,
                    p.status,
                    p.dueDate,
                    p.updatedAt
                )
                FROM Project p
                WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                  AND p.status IS NOT NULL
                  AND p.status <> 'Archived'
            """)
    Page<ProjectDTO> findAllProjects(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
                SELECT p
                FROM Project p
                WHERE p.inviteLink = :inviteLink
                  AND p.allowLinkJoin = true
            """)
    Optional<Project> findActiveSharedProject(@Param("inviteLink") String inviteLink);

    // 🟢 Lấy top N project của 1 PM (theo userId hoặc email)
    @Query("""
                SELECT new com.devcollab.dto.ProjectDTO(
                    p.projectId,
                    p.name,
                    p.coverImage,
                    p.status
                )
                FROM Project p
                WHERE p.createdBy.email = :email
                  AND p.status IS NOT NULL
                  AND p.status <> 'Archived'
                ORDER BY p.updatedAt DESC
            """)
    List<ProjectDTO> findTopProjectsByPm(@Param("email") String email, Pageable pageable);

    @Query("""
              SELECT new com.devcollab.dto.ProjectDTO(
                  p.projectId,
                  p.name,
                  p.description,
                  p.coverImage,
                  p.status,
                  p.dueDate,
                  p.updatedAt
              )
              FROM Project p
              WHERE p.createdBy IS NOT NULL
                AND p.createdBy.email = :email
                AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                AND p.status IS NOT NULL
                AND p.status <> 'Archived'
            """)
    Page<ProjectDTO> findAllProjectsByPm(@Param("email") String email,
            @Param("keyword") String keyword, Pageable pageable);

    @Query("""
                SELECT new com.devcollab.dto.userTaskDto.ProjectFilterDTO(p.projectId, p.name)
                FROM Project p
                JOIN p.members pm
                WHERE pm.user.userId = :userId AND p.status = 'active'
                ORDER BY p.name ASC
            """)
    List<ProjectFilterDTO> findActiveProjectsByUser(@Param("userId") Long userId);

    // ✅ Use your stored procedure sp_GetProjectProgress
    @Query(value = "EXEC sp_GetProjectProgress :projectId", nativeQuery = true)
    Map<String, Object> getProjectProgress(Long projectId);

    // ✅ Metrics summary (Total, Done, Overdue, In Progress)
    @Query(value = """
            SELECT
                COUNT(*) AS totalTasks,
                SUM(CASE WHEN status='DONE' THEN 1 ELSE 0 END) AS completedTasks,
                SUM(CASE WHEN status<>'DONE' AND deadline < SYSUTCDATETIME() THEN 1 ELSE 0 END) AS overdueTasks,
                SUM(CASE WHEN status='IN_PROGRESS' THEN 1 ELSE 0 END) AS activeTasks
            FROM Task
            WHERE project_id = :projectId
            """,
            nativeQuery = true)
    Map<String, Object> getProjectMetrics(Long projectId);

    boolean existsByNameIgnoreCaseAndCreatedBy_UserId(String name, Long creatorId);

    boolean existsByNameIgnoreCaseAndCreatedBy_UserIdAndProjectIdNot(String name, Long creatorId,
            Long projectId);

    @Query("""
                SELECT p FROM ProjectMember pm
                JOIN pm.project p
                JOIN pm.user u
                WHERE u.email = :email
                ORDER BY pm.joinedAt DESC
            """)
    Page<Project> findByUserEmail(@Param("email") String email, Pageable pageable);

}
