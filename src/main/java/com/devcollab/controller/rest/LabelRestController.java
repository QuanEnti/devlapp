package com.devcollab.controller.rest;

import com.devcollab.domain.User;
import com.devcollab.dto.LabelDTO;
import com.devcollab.service.feature.LabelService;
import com.devcollab.service.system.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LabelRestController {

    private final LabelService labelService;
    private final AuthService authService;

    @GetMapping("/labels")
    public ResponseEntity<?> getLabelsByProject(@RequestParam Long projectId,
            @RequestParam(required = false) String keyword) {

        return ResponseEntity.ok(labelService.getLabelsByProject(projectId, keyword));
    }

    @GetMapping("/labels/{labelId}")
    public ResponseEntity<?> getLabelById(@PathVariable Long labelId) {
        try {
            LabelDTO dto = labelService.getLabelById(labelId);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/labels")
    public ResponseEntity<?> createLabel(@RequestParam Long projectId, @RequestParam String name,
            @RequestParam(required = false) String color, Authentication auth) {

        try {
            User actor = authService.getCurrentUserEntity(auth);
            LabelDTO dto = labelService.createLabel(projectId, name, color, actor);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/labels/{labelId}")
    public ResponseEntity<?> updateLabel(@PathVariable Long labelId,
            @RequestBody Map<String, String> body, Authentication auth) {

        try {
            String name = body.get("name");
            String color = body.get("color");

            User actor = authService.getCurrentUserEntity(auth);
            LabelDTO dto = labelService.updateLabel(labelId, name, color, actor);

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/labels/{labelId}")
    public ResponseEntity<?> deleteLabel(@PathVariable Long labelId, Authentication auth) {

        try {
            User actor = authService.getCurrentUserEntity(auth);
            labelService.deleteLabel(labelId, actor);
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
    public ResponseEntity<?> assignLabel(@PathVariable Long taskId, @PathVariable Long labelId,
            Authentication auth) {

        try {
            User actor = authService.getCurrentUserEntity(auth);
            labelService.assignLabelToTask(taskId, labelId, actor);

            return ResponseEntity.ok(Map.of("message", "✅ Label assigned successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/tasks/{taskId}/labels/{labelId}")
    public ResponseEntity<?> unassignLabel(@PathVariable Long taskId, @PathVariable Long labelId,
            Authentication auth) {

        try {
            User actor = authService.getCurrentUserEntity(auth);
            labelService.removeLabelFromTask(taskId, labelId, actor);

            return ResponseEntity.ok(Map.of("message", "🗑️ Label removed from task"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
