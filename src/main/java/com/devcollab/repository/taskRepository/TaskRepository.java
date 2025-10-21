package com.devcollab.repository.taskRepository;

import com.devcollab.domain.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
