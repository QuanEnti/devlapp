package com.devcollab.controller.rest;

import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.dto.TaskDTO;
import com.devcollab.dto.TaskFollowerDTO;
import com.devcollab.dto.UserDTO;
import com.devcollab.dto.request.MoveTaskRequest;
import com.devcollab.dto.request.TaskQuickCreateReq;
import com.devcollab.dto.userTaskDto.ProjectFilterDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.service.core.ProjectService;
import com.devcollab.service.core.TaskService;
import com.devcollab.service.core.TaskFollowerService;
import com.devcollab.service.system.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class TaskRestController {

    private final TaskService taskService;
    private final AuthService authService;
    private final TaskFollowerService taskFollowerService;
    private final ProjectService projectService;

    // ============================ GET TASKS BY PROJECT
    // ============================
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskDTO>> getTasksByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
    }

    // ============================ QUICK CREATE TASK ============================
    @PostMapping("/quick")
    public ResponseEntity<TaskDTO> quickCreate(@RequestBody TaskQuickCreateReq req,
            Authentication auth) {
        if (req == null || req.title() == null || req.title().isBlank() || req.columnId() == null
                || req.projectId() == null) {
            return ResponseEntity.badRequest().build();
        }

        UserDTO current = authService.getCurrentUser(auth);
        if (current == null || current.getUserId() == null) {
            return ResponseEntity.status(401).build();
        }

        Task saved = taskService.quickCreate(req.title(), req.columnId(), req.projectId(),
                current.getUserId());

        return ResponseEntity.status(201).body(TaskDTO.fromEntity(saved));
    }

    // ============================ DELETE TASK (PM + CREATOR)
    // ============================
    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId, Authentication auth) {

        UserDTO current = authService.getCurrentUser(auth);
        if (current == null)
            return ResponseEntity.status(401).build();

        User actor = new User();
        actor.setUserId(current.getUserId());
        actor.setEmail(current.getEmail());

        try {
            taskService.deleteTask(taskId, actor);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getByIdAsDTO(taskId));
    }

    @PutMapping("/{taskId}/description")
    public ResponseEntity<TaskDTO> updateDescription(@PathVariable Long taskId,
            @RequestBody Map<String, Object> payload) {
        String desc = (String) payload.getOrDefault("description_md", "");
        TaskDTO updated = taskService.updateTaskDescription(taskId, desc);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{taskId}/members")
    public ResponseEntity<List<TaskFollowerDTO>> getTaskMembers(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskFollowerService.getFollowersByTask(taskId));
    }

    @PutMapping("/{taskId}/assign/{userId}")
    public ResponseEntity<Map<String, String>> assignMember(@PathVariable Long taskId,
            @PathVariable Long userId) {
        // Set assignee_id in Task table
        try {
            taskService.assignTask(taskId, userId);
        } catch (Exception e) {
            // Log but don't fail if assignee update fails
            System.err.println("Failed to set assignee: " + e.getMessage());
        }
        
        // Also add to followers
        boolean added = taskFollowerService.assignMember(taskId, userId);
        if (added) {
            return ResponseEntity.ok(Map.of("message", " Member assigned successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", " Member already assigned"));
        }
    }

    @PutMapping("/{taskId}/unassign/{userId}")
    public ResponseEntity<Map<String, String>> unassignMember(@PathVariable Long taskId,
            @PathVariable Long userId) {
        // Check if this user is the current assignee and clear assignee_id if so
        try {
            Task task = taskService.getById(taskId);
            if (task.getAssignee() != null && task.getAssignee().getUserId().equals(userId)) {
                // Clear assignee by setting it to null
                task.setAssignee(null);
                task.setUpdatedAt(LocalDateTime.now());
                taskService.updateTask(taskId, task);
            }
        } catch (Exception e) {
            // Log but don't fail if assignee clear fails
            System.err.println("Failed to clear assignee: " + e.getMessage());
        }
        
        // Remove from followers
        boolean removed = taskFollowerService.unassignMember(taskId, userId);
        if (removed) {
            return ResponseEntity.ok(Map.of("message", " Member unassigned"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", " Member not assigned"));
        }
    }

    @PutMapping("/{taskId}/dates")
    public ResponseEntity<TaskDTO> updateTaskDates(@PathVariable Long taskId,
            @RequestBody TaskDTO dto) {
        return ResponseEntity.ok(taskService.updateDates(taskId, dto));
    }

    @PutMapping("/{taskId}/move")
    public ResponseEntity<?> moveTask(@PathVariable Long taskId, @RequestBody MoveTaskRequest req) {
        try {
            TaskDTO updated = taskService.moveTask(taskId, req);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{taskId}/archive")
    public ResponseEntity<?> archiveTask(@PathVariable Long taskId, Authentication auth) {

        UserDTO current = authService.getCurrentUser(auth);
        if (current == null)
            return ResponseEntity.status(401).build();

        User actor = new User();
        actor.setUserId(current.getUserId());
        actor.setEmail(current.getEmail());

        try {
            taskService.archiveTask(taskId, actor);
            return ResponseEntity.ok(Map.of("message", "🗃️ Task archived"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{taskId}/restore")
    public ResponseEntity<?> restoreTask(@PathVariable Long taskId, Authentication auth) {

        UserDTO current = authService.getCurrentUser(auth);
        if (current == null)
            return ResponseEntity.status(401).build();

        User actor = new User();
        actor.setUserId(current.getUserId());
        actor.setEmail(current.getEmail());

        try {
            taskService.restoreTask(taskId, actor);
            return ResponseEntity.ok(Map.of("message", "♻️ Task restored"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{taskId}/complete")
    public ResponseEntity<?> markComplete(@PathVariable Long taskId, Authentication auth) {
        UserDTO current = authService.getCurrentUser(auth);
        if (current == null)
            return ResponseEntity.status(401).body(Map.of("message", "❌ Bạn chưa đăng nhập"));

        try {
            return ResponseEntity.ok(taskService.markComplete(taskId, current.getUserId()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{taskId}/incomplete")
    public ResponseEntity<?> markIncomplete(
            @PathVariable Long taskId,
            Authentication auth) {

        UserDTO current = authService.getCurrentUser(auth);
        if (current == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "message", "Bạn chưa đăng nhập"));
        }

        try {
            TaskDTO result = taskService.markIncomplete(
                    taskId,
                    current.getUserId(),
                    current.getEmail());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "task", result,
                    "message", "Task đã được chuyển về trạng thái OPEN"));

        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of(
                    "status", "forbidden",
                    "message", ex.getMessage()));

        } catch (NotFoundException ex) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "not_found",
                    "message", ex.getMessage()));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Lỗi hệ thống",
                    "detail", ex.getMessage()));
        }
    }

    @PutMapping("/{taskId}/deadline/remove")
    public ResponseEntity<?> removeDeadline(@PathVariable Long taskId, Authentication auth) {

        UserDTO current = authService.getCurrentUser(auth);
        if (current == null)
            return ResponseEntity.status(401).build();

        User actor = new User();
        actor.setUserId(current.getUserId());
        actor.setEmail(current.getEmail());

        try {
            taskService.removeDeadline(taskId, actor);
            return ResponseEntity.ok(Map.of("message", "🗑️ Deadline removed"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/user/projects")
    public ResponseEntity<List<ProjectFilterDTO>> getUserProjects(Authentication auth) {
        UserDTO current = authService.getCurrentUser(auth);
        if (current == null || current.getUserId() == null) {
            return ResponseEntity.status(401).build();
        }

        List<ProjectFilterDTO> projects = projectService.getActiveProjectsForUser(current.getUserId());

        return ResponseEntity.ok(projects);
    }
}
