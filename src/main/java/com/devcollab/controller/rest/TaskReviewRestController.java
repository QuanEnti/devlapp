package com.devcollab.controller.rest;

import com.devcollab.dto.TaskDTO;
import com.devcollab.dto.TaskDetailDTO;
import com.devcollab.dto.TaskReviewDTO;
import com.devcollab.service.core.TaskService;
import com.devcollab.service.feature.CommentService;
import com.devcollab.dto.CommentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pm/review")
@RequiredArgsConstructor
public class TaskReviewRestController {

    private final TaskService taskService;
    private final CommentService commentService;

    // 🟢 Get all tasks in a project
    @GetMapping("/tasks")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<?> getProjectTasksForReview(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        try {
            return ResponseEntity.ok(
                    taskService.getTasksForReviewPaged(projectId, page, size, status, search)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error loading tasks");
        }
    }


    // 🟢 Get task detail (info + description)
    @GetMapping("/tasks/{taskId}")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<TaskDetailDTO> getTaskDetailForReview(@PathVariable Long taskId) {
        try {
            TaskDetailDTO task = taskService.getTaskDetailForReview(taskId);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    // 🟢 Get all comments for a task
    @GetMapping("/tasks/{taskId}/comments")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable Long taskId) {
        try {
            List<CommentDTO> comments = commentService.getCommentsByTask(taskId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    // 🟢 Optionally mark task as reviewed/done - FIXED: removed userId parameter
    @PutMapping("/tasks/{taskId}/complete")
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<TaskDTO> markComplete(@PathVariable Long taskId) {
        try {
            // You might want to get the current user from security context
            // Long userId = getCurrentUserId();
            TaskDTO task = taskService.markComplete(taskId, null); // Adjust based on your service method
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}