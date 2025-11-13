package com.devcollab.controller.rest;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.devcollab.domain.User;
import com.devcollab.service.feature.CheckListService;
import com.devcollab.service.system.AuthService;
import java.util.Map;

@RestController
@RequestMapping("/api/checklists")
@RequiredArgsConstructor
public class CheckListRestController {

    private final CheckListService checkListService;
    private final AuthService authService;

    @GetMapping("/task/{taskId}")
    public ResponseEntity<?> getChecklist(@PathVariable Long taskId) {
        return ResponseEntity.ok(checkListService.getByTask(taskId));
    }

    @PostMapping("/task/{taskId}")
    public ResponseEntity<?> addItem(@PathVariable Long taskId,
            @RequestBody Map<String, String> body, Authentication auth) {

        User actor = authService.getCurrentUserEntity(auth);

        String content = body.get("item");
        return ResponseEntity.ok(checkListService.addItem(taskId, content, actor));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggleItem(@PathVariable Long id,
            @RequestBody Map<String, Boolean> body, Authentication auth) {

        User actor = authService.getCurrentUserEntity(auth);

        boolean done = body.getOrDefault("isDone", false);
        return ResponseEntity.ok(checkListService.toggleItem(id, done, actor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id, Authentication auth) {

        User actor = authService.getCurrentUserEntity(auth);

        checkListService.deleteItem(id, actor);
        return ResponseEntity.ok(Map.of("message", "Đã xóa checklist item"));
    }
}

