package com.devcollab.controller.rest;

import com.devcollab.dto.CommentDTO;
import com.devcollab.domain.User;
import com.devcollab.service.feature.CommentService;
import com.devcollab.service.system.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class CommentRestController {

    private final CommentService commentService;
    private final AuthService authService;

    // 🟩 Lấy toàn bộ comment + replies của 1 task
    @GetMapping
    public List<CommentDTO> getComments(@PathVariable Long taskId) {
        return commentService.getCommentsByTask(taskId);
    }

    @PostMapping
    public CommentDTO addComment(@PathVariable Long taskId, @RequestBody Map<String, String> body,
            Authentication auth) {
        User user = authService.getCurrentUserEntity(auth);
        if (user == null)
            throw new SecurityException("Unauthorized");

        String content = body.get("content");
        if (content == null || content.isBlank())
            throw new IllegalArgumentException("Content cannot be empty");

        String mentionsJson = body.getOrDefault("mentionsJson", null);
        return commentService.addComment(taskId, user.getUserId(), content, mentionsJson);
    }

    // 🔁 Reply tới 1 comment
    @PostMapping("/{parentId}/reply")
    public CommentDTO replyToComment(@PathVariable Long taskId, @PathVariable Long parentId,
            @RequestBody Map<String, String> payload, Authentication auth) {

        User currentUser = authService.getCurrentUserEntity(auth);
        if (currentUser == null)
            throw new SecurityException("Unauthorized");

        String content = payload.get("content");
        if (content == null || content.isBlank())
            throw new IllegalArgumentException("Reply content cannot be empty");

        return commentService.replyToComment(parentId, currentUser.getUserId(), content);
    }


    @DeleteMapping("/{commentId}")
    public void deleteComment(@PathVariable Long taskId, @PathVariable Long commentId,
            Authentication auth) {

        User currentUser = authService.getCurrentUserEntity(auth);
        if (currentUser == null)
            throw new SecurityException("Unauthorized");

        commentService.deleteComment(commentId, currentUser.getUserId());
    }

    // ✏️ Cập nhật nội dung comment
    @PutMapping("/{commentId}")
    public CommentDTO updateComment(@PathVariable Long taskId, @PathVariable Long commentId,
            @RequestBody Map<String, String> body, Authentication auth) {

        User currentUser = authService.getCurrentUserEntity(auth);
        if (currentUser == null)
            throw new SecurityException("Unauthorized");

        String newContent = body.get("content");
        if (newContent == null || newContent.isBlank())
            throw new IllegalArgumentException("Content cannot be empty");

        return commentService.updateComment(commentId, currentUser.getUserId(), newContent);
    }

}
