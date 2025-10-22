package com.devcollab.repository.taskRepository;

import com.devcollab.domain.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>
{
    /**
     * Lấy Task kèm TẤT CẢ các quan hệ của nó trong 1 câu query
     * Bằng cách dùng EntityGraph, chúng ta tránh được N+1 query.
     */
    @EntityGraph(attributePaths = {
            "project",
            "sprint",
            "column",
            "assignee",
            "createdBy",
            "labels",
            "checklists",    // Quan trọng
            "comments",      // Quan trọng
            "comments.user"  // Lấy cả user của comment
            // "attachments" // Thêm nếu cần
    })
    Optional<Task> findDetailedById(Long taskId);

    // Lấy tất cả Task trong một Project, kèm thông tin Assignee, sắp xếp theo cột và thứ tự
    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.assignee " +
            "WHERE t.project.projectId = :projectId " +
            "ORDER BY t.column.orderIndex, t.orderIndex")
    List<Task> findAllByProjectId(@Param("projectId") Long projectId);


    // Lấy tất cả Task trong một Project
    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.assignee " +
            "WHERE t.project.projectId = :projectId AND t.assignee.userId = :assigneeId " +
            "ORDER BY t.column.orderIndex, t.orderIndex")
    List<Task> findByProjectIdAndAssigneeId(
            @Param("projectId") Long projectId,
            @Param("assigneeId") Long assigneeId
    );
}
