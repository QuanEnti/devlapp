package com.devcollab.controller.rest;

import com.devcollab.domain.Label;
import com.devcollab.repository.TaskRepository;
import com.devcollab.service.feature.LabelService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class LabelRestController {

    private final LabelService labelService;
    private final TaskRepository taskRepository;

    public LabelRestController(LabelService labelService, TaskRepository taskRepository) {
        this.labelService = labelService;
        this.taskRepository = taskRepository;
    }
    
    // 🔹 Lấy danh sách label của 1 project
    @GetMapping("/labels")
    public ResponseEntity<?> getLabelsByProject(
            @RequestParam Long projectId,
            @RequestParam(required = false) String keyword) {

        return ResponseEntity.ok(labelService.getLabelsByProject(projectId, keyword));
    }

    // 🔹 Tạo mới label
    @PostMapping("/labels")
    public ResponseEntity<?> createLabel(@RequestParam Long projectId,
            @RequestParam String name,
            @RequestParam(required = false) String color) {
        try {
            return ResponseEntity.ok(labelService.createLabel(projectId, name, color));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 🔹 Cập nhật label
    @PutMapping("/labels/{labelId}")
    public ResponseEntity<?> updateLabel(@PathVariable Long labelId,
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        String color = body.get("color");
        try {
            return ResponseEntity.ok(labelService.updateLabel(labelId, name, color));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 🔹 Lấy chi tiết 1 label theo ID (cho popup Edit Label)
    @GetMapping("/labels/{labelId}")
    public ResponseEntity<?> getLabelById(@PathVariable Long labelId) {
        try {
            return ResponseEntity.ok(labelService.getLabelById(labelId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 🔹 Xóa label
    @DeleteMapping("/labels/{labelId}")
    public ResponseEntity<?> deleteLabel(@PathVariable Long labelId) {
        try {
            labelService.deleteLabel(labelId);
            return ResponseEntity.ok(Map.of("message", "Label deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tasks/{taskId}/labels")
    public ResponseEntity<?> getLabelsOfTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(labelService.getLabelsByTask(taskId));
    }

    @PostMapping("/tasks/{taskId}/labels/{labelId}")
    public ResponseEntity<?> assignLabel(@PathVariable Long taskId, @PathVariable Long labelId) {
        try {
            labelService.assignLabelToTask(taskId, labelId);
            return ResponseEntity.ok(Map.of("message", "✅ Label assigned successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/tasks/{taskId}/labels/{labelId}")
    public ResponseEntity<?> unassignLabel(@PathVariable Long taskId, @PathVariable Long labelId) {
        try {
            labelService.removeLabelFromTask(taskId, labelId);
            return ResponseEntity.ok(Map.of("message", "🗑️ Label unassigned successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}
