package com.devcollab.repository;

import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.dto.userTaskDto.TaskCardDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProject_ProjectId(Long projectId);

    List<Task> findByAssignee_UserId(Long userId);

    List<Task> findByStatus(String status);
    
   

    List<Task> findByProject_ProjectIdAndAssignee_UserId(Long projectId, Long userId);
    long countByProject_ProjectId(Long projectId);
    long countByProject_ProjectIdAndStatus(Long projectId, String status);

    @Query("""
           select count(t) from Task t
           where t.project.projectId = :projectId
             and t.deadline is not null
             and t.deadline < :now
             and upper(t.status) <> 'CLOSED'
           """)
    long countOverdue(Long projectId, LocalDateTime now);

    @Query("""
            SELECT
                FORMAT(t.closedAt, 'ddd') AS dayOfWeek,
                COUNT(t)
            FROM Task t
            WHERE t.project.projectId = :projectId
              AND t.status = 'DONE'
              AND t.closedAt BETWEEN :start AND :end
            GROUP BY FORMAT(t.closedAt, 'ddd')
            ORDER BY MIN(t.closedAt)
        """)
    List<Object[]> countCompletedTasksPerDay(
        @Param("projectId") Long projectId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end);

    @Query(value = """
            SELECT DATENAME(MONTH, closed_at) AS monthName, COUNT(*) AS total
            FROM Task
            WHERE project_id = :projectId
              AND status = 'DONE'
              AND closed_at IS NOT NULL
              AND closed_at >= DATEADD(MONTH, -6, GETDATE())
            GROUP BY DATENAME(MONTH, closed_at), MONTH(closed_at)
            ORDER BY MONTH(closed_at)
        """, nativeQuery = true)
    List<Object[]> countCompletedTasksPerMonth(@Param("projectId") Long projectId);
    
    List<Task> findByProject_ProjectIdAndArchivedFalse(Long projectId);
    
    @Query("SELECT t FROM Task t WHERE t.deadline IS NOT NULL AND t.status <> 'DONE' AND t.deadline > :now")
    List<Task> findTasksWithUpcomingDeadlines(@Param("now") LocalDateTime now);
    
    @Query("""
            SELECT DISTINCT t FROM Task t
            LEFT JOIN FETCH t.assignee
            LEFT JOIN FETCH t.createdBy
            LEFT JOIN FETCH t.followers f
            LEFT JOIN FETCH f.user
            WHERE t.deadline IS NOT NULL
              AND t.status <> 'DONE'
              AND t.deadline BETWEEN :from AND :to
        """)
    List<Task> findTasksDueBetween(@Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);
    @Query(value = """
        SELECT
            t.task_id AS id,
            t.title,
            t.status,
            t.priority,
            t.created_at AS createdAt,
            t.deadline,
            creator.name AS creatorName,
            assignee.avatar_url AS assigneeAvatarUrl,

            -- Dùng Correlated Subquery (Hiệu năng cao)

        FROM
            Task AS t
        -- JOIN để lọc bảo mật (dựa trên Entity)
        INNER JOIN
            Project AS p ON t.project_id = p.project_id
        INNER JOIN
            ProjectMember AS pm ON p.project_id = pm.project_id
        -- JOIN để lấy thông tin
        LEFT JOIN
            [User] AS creator ON t.created_by = creator.user_id
        LEFT JOIN
            [User] AS assignee ON t.assignee_id = assignee.user_id
        WHERE
            t.assignee_id = :userId       -- 1. Gán cho tôi
            AND p.status = 'active'       -- 2. Project đang 'active'
            AND pm.user_id = :userId      -- 3. Tôi là thành viên project

            -- 4. Bộ lọc Project (NULL = lấy tất cả)
            AND (:projectId IS NULL OR p.project_id = :projectId) 

            -- 5. Bộ lọc Status (NULL = lấy tất cả)
            AND (
                :statuses IS NULL 
                OR t.status IN (SELECT value FROM STRING_SPLIT(:statuses, ','))
            )
        """, nativeQuery = true)
    List<TaskCardDTO> findUserTasks(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId,
            @Param("statuses") String statuses
    );
    @Query("SELECT t FROM Task t WHERE t.assignee = :user ORDER BY t.deadline ASC")
    List<Task> findTasksByAssignee(@Param("user") User user);

    // ✅ Tasks followed by the user
    @Query("SELECT tf.task FROM TaskFollower tf WHERE tf.user = :user")
    List<Task> findTasksFollowedByUser(@Param("user") User user);

    // ✅ Tasks created by the user (optional)
    @Query("SELECT t FROM Task t WHERE t.createdBy = :user ORDER BY t.createdAt DESC")
    List<Task> findTasksCreatedBy(@Param("user") User user);
}

