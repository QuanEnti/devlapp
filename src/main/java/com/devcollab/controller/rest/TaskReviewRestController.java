package com.devcollab.controller.rest;
import com.devcollab.dto.TaskDTO;
import com.devcollab.service.core.TaskService;
import com.devcollab.service.feature.CommentService;
import com.devcollab.dto.CommentDTO;
import lombok.RequiredArgsConstructor;
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
    public List<TaskDTO> getProjectTasks(@RequestParam Long projectId) {
        return taskService.getTasksByProject(projectId);
    }

    // 🟢 Get task detail (info + description)
    @GetMapping("/tasks/{taskId}")
    @PreAuthorize("hasRole('PM')")
    public TaskDTO getTaskDetail(@PathVariable Long taskId) {
        return taskService.getByIdAsDTO(taskId);
    }

    // 🟢 Get all comments for a task
    @GetMapping("/tasks/{taskId}/comments")
    @PreAuthorize("hasRole('PM')")
    public List<CommentDTO> getComments(@PathVariable Long taskId) {
        return commentService.getCommentsByTask(taskId);
    }

    // 🟢 Optionally mark task as reviewed/done
    @PostMapping("/tasks/{taskId}/complete")
    @PreAuthorize("hasRole('PM')")
    public TaskDTO markComplete(@PathVariable Long taskId, @RequestParam Long userId) {
        return taskService.markComplete(taskId, userId);
    }
}

