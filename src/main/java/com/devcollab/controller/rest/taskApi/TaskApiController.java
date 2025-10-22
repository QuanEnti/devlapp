package com.devcollab.controller.rest.taskApi;

import com.devcollab.dto.taskDto.*;
import com.devcollab.service.taskService.TaskService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskApiController {
    private final TaskService taskService;

    /**
     * Endpoint để lấy chi tiết một task
     * Ví dụ: GET /api/tasks/123
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTaskById(
            @PathVariable Long taskId,
            @RequestAttribute("userId") Long currentUserId // Lấy từ JwtAuthenticationFilter
    ) {
        try {
            // Gọi service với ID task và ID user (để check quyền)
            TaskDetailDto taskDetail = taskService.getTaskDetails(taskId, currentUserId);
            return ResponseEntity.ok(taskDetail);

        } catch (EntityNotFoundException e) {
            // Nếu service ném lỗi không tìm thấy
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (AccessDeniedException e) {
            // Nếu service ném lỗi không có quyền
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());

        } catch (Exception e) {
            // Các lỗi khác
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * (POST) THÊM COMMENT MỚI
     */
    @PostMapping("/{taskId}/comments")
    public ResponseEntity<?> addCommentToTask(
            @PathVariable Long taskId,
            @RequestAttribute("userId") Long currentUserId,
            @Valid @RequestBody CommentRequestDto commentRequest // @Valid để kích hoạt validation
    ) {
        try {
            CommentDto newComment = taskService.addComment(taskId, currentUserId, commentRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(newComment);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * (POST) THÊM FILE ĐÍNH KÈM
     */
    @PostMapping("/{taskId}/attachments")
    public ResponseEntity<?> addAttachmentToTask(
            @PathVariable Long taskId,
            @RequestAttribute("userId") Long currentUserId,
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File cannot be empty");
        }
        try {
            AttachmentDto newAttachment = taskService.addAttachment(taskId, currentUserId, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(newAttachment);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * (PATCH) CẬP NHẬT STATUS (ví dụ: "DONE" / "Completed")
     */
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<?> updateTaskStatus(
            @PathVariable Long taskId,
            @RequestAttribute("userId") Long currentUserId,
            @Valid @RequestBody TaskStatusUpdateDto statusUpdate // @Valid để kích hoạt validation
    ) {
        try {
            // Khi update status, ta trả về cả DTO chi tiết (đã cập nhật)
            TaskDetailDto updatedTask = taskService.updateTaskStatus(taskId, currentUserId, statusUpdate);
            return ResponseEntity.ok(updatedTask);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
