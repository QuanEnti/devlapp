package com.devcollab.repository;

import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.dto.userTaskDto.TaskCardDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    @Query("SELECT t FROM Task t WHERE t.assignee = :user ORDER BY t.deadline ASC")
    List<Task> findTasksByAssignee(@Param("user") User user);

    @Query("""
    SELECT DISTINCT t FROM Task t
    LEFT JOIN t.followers f
    WHERE t.assignee = :user OR t.createdBy = :user OR f.user = :user
""")
    List<Task> findAllUserTasks(@Param("user") User user);

    static String SCOPE = """
        (t.assignee = :user OR t.createdBy = :user OR
         EXISTS (SELECT 1 FROM TaskFollower tf WHERE tf.task = t AND tf.user = :user))
    """;

    // ----- ORDER BY DEADLINE (NULLS LAST)
    @Query("""
        SELECT t FROM Task t
        WHERE """ + SCOPE + """
        ORDER BY CASE WHEN t.deadline IS NULL THEN 1 ELSE 0 END,
                 t.deadline ASC
    """)
    Page<Task> findUserTasksOrderByDeadline(@Param("user") User user, Pageable pageable);

    // ----- ORDER BY PRIORITY (HIGH > MEDIUM > LOW)
    @Query("""
        SELECT t FROM Task t
        WHERE """ + SCOPE + """
        ORDER BY CASE t.priority
                   WHEN 'CRITICAL' THEN 1
                   WHEN 'HIGH' THEN 2
                   WHEN 'MEDIUM' THEN 3
                   WHEN 'LOW' THEN 4
                   ELSE 5
                 END
    """)
    Page<Task> findUserTasksOrderByPriority(@Param("user") User user, Pageable pageable);

    // ----- ORDER BY PROJECT NAME (NULL LAST)
    @Query("""
        SELECT t FROM Task t
        LEFT JOIN t.project p
        WHERE """ + SCOPE + """
        ORDER BY CASE WHEN p.name IS NULL THEN 1 ELSE 0 END,
                 p.name ASC
    """)
    Page<Task> findUserTasksOrderByProject(@Param("user") User user, Pageable pageable);

    @Query("""
        SELECT t FROM Task t
        WHERE (t.assignee = :user OR t.createdBy = :user 
           OR EXISTS (
               SELECT 1 FROM TaskFollower tf WHERE tf.task = t AND tf.user = :user
           ))
           AND t.status = :status
        ORDER BY CASE WHEN t.deadline IS NULL THEN 1 ELSE 0 END,
                 t.deadline ASC
        """)
    Page<Task> findUserTasksByStatus(@Param("user") User user,
                                     @Param("status") String status,
                                     Pageable pageable);

    // ✅ Tasks created by the user (optional)
    @Query("SELECT t FROM Task t WHERE t.createdBy = :user ORDER BY t.createdAt DESC")
    List<Task> findTasksCreatedBy(@Param("user") User user);

    @Query("""
    SELECT t.status AS status, COUNT(t) AS count
    FROM Task t
    WHERE t.project.projectId = :projectId AND t.archived = false
    GROUP BY t.status
""")
    List<Map<String, Object>> countTasksByStatus(@Param("projectId") Long projectId);
    @Query(value = """
    SELECT 
        u.user_id AS userId,
        u.name AS name,
        u.email AS email,
        COUNT(DISTINCT t.task_id) AS totalTasks,
        SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END) AS completedTasks,
        SUM(CASE 
            WHEN t.status = 'DONE' 
                 AND t.closed_at IS NOT NULL 
                 AND t.deadline IS NOT NULL 
                 AND t.closed_at <= t.deadline 
            THEN 1 ELSE 0 END) AS onTimeTasks,
        SUM(CASE 
            WHEN t.status = 'DONE' 
                 AND t.closed_at IS NOT NULL 
                 AND t.deadline IS NOT NULL 
                 AND t.closed_at > t.deadline 
            THEN 1 ELSE 0 END) AS lateTasks,
        ISNULL(AVG(
            CASE 
                WHEN t.status = 'DONE' AND t.deadline IS NOT NULL AND t.closed_at IS NOT NULL 
                THEN DATEDIFF(HOUR, t.deadline, t.closed_at)
                ELSE NULL
            END
        ), 0) AS avgDelayHours,
        SUM(CASE 
            WHEN t.status = 'DONE' AND t.priority = 'HIGH' THEN 3
            WHEN t.status = 'DONE' AND t.priority = 'MEDIUM' THEN 2
            WHEN t.status = 'DONE' AND t.priority = 'LOW' THEN 1
            ELSE 0 END) AS priorityPoints
    FROM [Task] t
    JOIN [TaskFollower] tf ON t.task_id = tf.task_id
    JOIN [User] u ON tf.user_id = u.user_id
    WHERE t.project_id = :projectId
    GROUP BY u.user_id, u.name, u.email
    ORDER BY completedTasks DESC
""", nativeQuery = true)
    List<Object[]> findMemberPerformanceByProject(@Param("projectId") Long projectId);
}

