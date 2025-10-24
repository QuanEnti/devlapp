package com.devcollab.controller.rest;

import com.devcollab.domain.Task;
import com.devcollab.dto.TaskDTO;
import com.devcollab.dto.TaskFollowerDTO;
import com.devcollab.dto.UserDTO;
import com.devcollab.dto.request.MoveTaskRequest;
import com.devcollab.dto.request.TaskDatesUpdateReq;
import com.devcollab.dto.request.TaskQuickCreateReq;
import com.devcollab.service.core.TaskService;
import com.devcollab.service.core.TaskFollowerService; // ✅ Thêm service cho member
import com.devcollab.service.system.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class TaskRestController {

    private final TaskService taskService;
    private final AuthService authService;
    private final TaskFollowerService taskFollowerService; // ✅ Thêm service cho assign/unassign

    // ====================== TASK CRUD ======================

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskDTO>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(
                taskService.getTasksByProject(projectId)
                        .stream().map(TaskDTO::fromEntity).toList());
    }

    @PostMapping("/quick")
    public ResponseEntity<TaskDTO> quickCreate(
            @RequestBody TaskQuickCreateReq req,
            Authentication auth) {
        if (req == null || req.title() == null || req.title().isBlank()
                || req.columnId() == null || req.projectId() == null) {
            return ResponseEntity.badRequest().build();
        }

        UserDTO current = authService.getCurrentUser(auth);
        if (current == null || current.getUserId() == null) {
            return ResponseEntity.status(401).build();
        }

        Task saved = taskService.quickCreate(
                req.title(),
                req.columnId(),
                req.projectId(),
                current.getUserId());

        return ResponseEntity.status(201).body(TaskDTO.fromEntity(saved));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long taskId) {
        Task task = taskService.getById(taskId);
        return ResponseEntity.ok(TaskDTO.fromEntity(task));
    }

    @PutMapping("/{taskId}/description")
    public ResponseEntity<TaskDTO> updateDescription(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> payload) {
        String desc = (String) payload.get("description_md");
        if (desc == null)
            desc = "";

        Task updated = taskService.updateTaskDescription(taskId, desc);
        return ResponseEntity.ok(TaskDTO.fromEntity(updated));
    }

    @GetMapping("/{taskId}/members")
        public ResponseEntity<List<TaskFollowerDTO>> getTaskMembers(@PathVariable Long taskId) {
            List<TaskFollowerDTO> members = taskFollowerService.getFollowersByTask(taskId);
            return ResponseEntity.ok(members);
        }

        @PutMapping("/{taskId}/assign/{userId}")
        public ResponseEntity<Map<String, String>> assignMember(@PathVariable Long taskId, @PathVariable Long userId) {
            boolean added = taskFollowerService.assignMember(taskId, userId);
            if (added) {
                return ResponseEntity.ok(Map.of("message", "✅ Member assigned successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "⚠️ Member is already assigned to this task"));
            }
        }

        @PutMapping("/{taskId}/unassign/{userId}")
        public ResponseEntity<Map<String, String>> unassignMember(@PathVariable Long taskId, @PathVariable Long userId) {
            boolean removed = taskFollowerService.unassignMember(taskId, userId);
            if (removed) {
                return ResponseEntity.ok(Map.of("message", "🗑️ Member unassigned successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "❌ Member not assigned to this task or already removed"));
            }
        }
       
        @PutMapping("/{taskId}/dates")
        public ResponseEntity<TaskDTO> updateTaskDates(
                @PathVariable Long taskId,
                @RequestBody TaskDTO dto) {

            TaskDTO updated = taskService.updateDates(taskId, dto);
            return ResponseEntity.ok(updated);
        }
        
        @PutMapping("/{taskId}/move")
        public ResponseEntity<?> moveTask(@PathVariable("taskId") Long taskId,
                @RequestBody MoveTaskRequest req) {
            try {
                TaskDTO updated = taskService.moveTask(taskId, req);
                return ResponseEntity.ok(updated);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", e.getMessage()));
            }
        }


}
