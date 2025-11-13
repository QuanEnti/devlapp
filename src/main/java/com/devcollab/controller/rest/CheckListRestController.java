package com.devcollab.controller.rest;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.devcollab.service.feature.CheckListService;
import java.util.Map;

@RestController
@RequestMapping("/api/checklists")
@RequiredArgsConstructor
public class CheckListRestController {

    private final CheckListService checkListService;

    /** 🧾 Lấy danh sách checklist của task */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<?> getChecklist(@PathVariable Long taskId) {
        return ResponseEntity.ok(checkListService.getByTask(taskId));
    }

    /** ➕ Thêm item mới vào checklist */
    @PostMapping("/task/{taskId}")
    public ResponseEntity<?> addItem(@PathVariable Long taskId,
            @RequestBody Map<String, String> body) {
        String content = body.get("item");
        return ResponseEntity.ok(checkListService.addItem(taskId, content));
    }

    /** ✅ Toggle hoàn thành / chưa hoàn thành */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggleItem(@PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean done = body.getOrDefault("isDone", false);
        return ResponseEntity.ok(checkListService.toggleItem(id, done));
    }

    /** ❌ Xóa một item trong checklist */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        checkListService.deleteItem(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa checklist item"));
    }
}
