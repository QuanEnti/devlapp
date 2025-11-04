package com.devcollab.controller.rest;

import com.devcollab.domain.Task;
import com.devcollab.dto.TaskDTO;
import com.devcollab.dto.TaskFollowerDTO;
import com.devcollab.dto.UserDTO;
import com.devcollab.dto.request.MoveTaskRequest;
import com.devcollab.dto.request.TaskDatesUpdateReq;
import com.devcollab.dto.request.TaskQuickCreateReq;
import com.devcollab.exception.NotFoundException;
import com.devcollab.service.core.TaskService;
import com.devcollab.service.core.TaskFollowerService; // ✅ Thêm service cho member
import com.devcollab.service.system.AuthService;
import com.devcollab.service.core.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import com.devcollab.dto.userTaskDto.TaskCardDTO;
import com.devcollab.dto.userTaskDto.ProjectFilterDTO;
import org.springframework.web.bind.annotation.RequestParam;
import com.devcollab.repository.TaskRepository;
import com.devcollab.repository.CommentRepository;
import com.devcollab.repository.AttachmentRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.domain.Comment;
import com.devcollab.domain.Attachment;
import com.devcollab.domain.User;
import com.devcollab.service.system.CloudinaryService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class TaskRestController {

    private final TaskService taskService;
    private final AuthService authService;
    private final TaskFollowerService taskFollowerService; // ✅ Thêm service cho assign/unassign
    private final ProjectService projectService;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    // ====================== TASK CRUD ======================

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskDTO>> getTasksByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
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
        return ResponseEntity.ok(taskService.getByIdAsDTO(taskId));
    }

    @PutMapping("/{taskId}/description")
    public ResponseEntity<TaskDTO> updateDescription(
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> payload) {
        String desc = (String) payload.get("description_md");
        if (desc == null)
            desc = "";
        TaskDTO updated = taskService.updateTaskDescription(taskId, desc);
        return ResponseEntity.ok(updated);
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

        @PutMapping("/{taskId}/archive")
        public ResponseEntity<Map<String, String>> archiveTask(@PathVariable Long taskId) {
            boolean archived = taskService.archiveTask(taskId);
            if (archived) {
                return ResponseEntity.ok(Map.of("message", "🗃️ Task archived successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "❌ Task not found"));
            }
        }

        @PutMapping("/{taskId}/restore")
        public ResponseEntity<Map<String, String>> restoreTask(@PathVariable Long taskId) {
            boolean restored = taskService.restoreTask(taskId);
            return restored
                    ? ResponseEntity.ok(Map.of("message", "♻️ Task restored successfully"))
                    : ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "❌ Task not found"));
        }

    // ====================== MODAL-SPECIFIC ENDPOINTS ======================

    @PostMapping("/{taskId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long taskId, @RequestBody Map<String, String> body, Authentication auth) {
        String content = body != null ? body.get("content") : null;
        if (content == null || content.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Missing content"));
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return ResponseEntity.notFound().build();
        String email = auth != null ? auth.getName() : null;
        if (email == null) return ResponseEntity.status(401).build();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();
        Comment c = new Comment();
        c.setTask(task);
        c.setUser(user);
        c.setContent(content);
        c.setCreatedAt(LocalDateTime.now());
        commentRepository.save(c);
        return ResponseEntity.status(201).body(Map.of("message", "Comment added"));
    }

    @PostMapping("/{taskId}/attachments")
    public ResponseEntity<?> uploadAttachment(@PathVariable Long taskId, @RequestParam("file") MultipartFile file, Authentication auth) {
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Missing file"));
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return ResponseEntity.notFound().build();
        String email = auth != null ? auth.getName() : null;
        if (email == null) return ResponseEntity.status(401).build();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();
        String url = cloudinaryService.uploadFile(file);
        Attachment a = new Attachment();
        a.setTask(task);
        a.setUploadedBy(user);
        a.setFileUrl(url);
        a.setFileName(file.getOriginalFilename());
        a.setMimeType(file.getContentType());
        a.setFileSize((int) file.getSize());
        a.setUploadedAt(LocalDateTime.now());
        attachmentRepository.save(a);
        return ResponseEntity.status(201).body(Map.of("message", "Attachment uploaded", "fileUrl", url));
    }

    @GetMapping("/user/my-tasks")
    public ResponseEntity<List<TaskCardDTO>> getMyTasks(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "statuses", required = false) String statuses,
            Authentication auth) {
        UserDTO current = authService.getCurrentUser(auth);
        if (current == null || current.getUserId() == null) {
            return ResponseEntity.status(401).build();
        }
        List<TaskCardDTO> tasks = taskService.getUserTasks(current.getUserId(), projectId, statuses);
        return ResponseEntity.ok(tasks);
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

        @PutMapping("/{taskId}/complete")
        public ResponseEntity<?> markComplete(
                @PathVariable Long taskId,
                Authentication auth) {

            UserDTO current = authService.getCurrentUser(auth);
            if (current == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "❌ Bạn chưa đăng nhập"));
            }

            try {
                TaskDTO dto = taskService.markComplete(taskId, current.getUserId());
                return ResponseEntity.ok(dto);
            } catch (SecurityException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", e.getMessage()));
            } catch (NotFoundException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "❌ Đã xảy ra lỗi khi đánh dấu hoàn thành"));
            }
        }

}
