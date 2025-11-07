package com.devcollab.service.impl.feature;

import com.devcollab.domain.Comment;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.dto.CommentDTO;
import com.devcollab.repository.CommentRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.feature.CommentService;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

        private final CommentRepository commentRepo;
        private final TaskRepository taskRepo;
        private final UserRepository userRepo;
        private final ActivityService activityService;
        private final NotificationService notificationService;

        private final ObjectMapper mapper = new ObjectMapper();

        // =========================================================
        // ✅ Thêm mới comment
        // =========================================================
        @Override
        public CommentDTO addComment(Long taskId, Long userId, String content, String mentionsJson) {
                Task task = taskRepo.findById(taskId)
                                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

                User user = userRepo.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

                // 🧩 Parse mentions nếu null → tự nhận diện từ content
                if (mentionsJson == null || mentionsJson.isBlank()) {
                        mentionsJson = detectMentionsFromContent(content);
                }

                Comment comment = new Comment();
                comment.setTask(task);
                comment.setUser(user);
                comment.setContent(content);
                comment.setMentionsJson(mentionsJson);

                Comment saved = commentRepo.save(comment);

                // 📝 Log activity
                activityService.log(
                                "TASK",
                                taskId,
                                "COMMENT_ADD",
                                "{\"commentId\":" + saved.getCommentId() +
                                                ",\"content\":\"" + escapeJson(content) + "\"}",
                                user);

                // 🔔 Gửi thông báo mention (nếu có)
                try {
                        List<CommentDTO> mentions = parseMentions(mentionsJson);
                        if (mentions != null && !mentions.isEmpty()) {
                                notificationService.notifyMentions(task, user, mentions);
                        }
                } catch (Exception e) {
                        log.error("⚠️ notifyMentions() failed: {}", e.getMessage(), e);
                }

                return toDTO(saved);
        }

        // =========================================================
        // ✅ Trả lời comment
        // =========================================================
        @Override
        public CommentDTO replyToComment(Long parentId, Long userId, String content) {
                Comment parent = commentRepo.findById(parentId)
                                .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));

                User user = userRepo.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

                Comment reply = new Comment();
                reply.setParent(parent);
                reply.setTask(parent.getTask());
                reply.setUser(user);
                reply.setContent(content);

                Comment saved = commentRepo.save(reply);

                // 📝 Log activity
                activityService.log(
                                "TASK",
                                parent.getTask().getTaskId(),
                                "COMMENT_REPLY",
                                "{\"replyId\":" + saved.getCommentId() +
                                                ",\"parentId\":" + parentId +
                                                ",\"content\":\"" + escapeJson(content) + "\"}",
                                user);

                return toDTO(saved);
        }

        // =========================================================
        // ✅ Lấy tất cả comment theo task (bao gồm reply)
        // =========================================================
        @Override
        public List<CommentDTO> getCommentsByTask(Long taskId) {
                List<Comment> roots = commentRepo.findByTask_TaskIdAndParentIsNullOrderByCreatedAtDesc(taskId);
                return roots.stream()
                                .map(this::toTreeDTO)
                                .collect(Collectors.toList());
        }

        // =========================================================
        // ✅ Xóa comment (chỉ chủ sở hữu mới được xóa)
        // =========================================================
        @Override
        public void deleteComment(Long commentId, Long userId) {
                Comment comment = commentRepo.findById(commentId)
                                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

                if (!comment.getUser().getUserId().equals(userId)) {
                        throw new SecurityException("You can only delete your own comments");
                }

                commentRepo.delete(comment);

                activityService.log(
                                "TASK",
                                comment.getTask().getTaskId(),
                                "COMMENT_DELETE",
                                "{\"commentId\":" + commentId + "}",
                                comment.getUser());
        }

        // =========================================================
        // ✅ Cập nhật nội dung comment
        // =========================================================
        @Override
        public CommentDTO updateComment(Long commentId, Long userId, String newContent) {
                Comment comment = commentRepo.findById(commentId)
                                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

                if (!comment.getUser().getUserId().equals(userId)) {
                        throw new SecurityException("You can only edit your own comments");
                }

                comment.setContent(newContent);
                Comment updated = commentRepo.save(comment);

                activityService.log(
                                "TASK",
                                comment.getTask().getTaskId(),
                                "COMMENT_EDIT",
                                "{\"commentId\":" + commentId +
                                                ",\"content\":\"" + escapeJson(newContent) + "\"}",
                                comment.getUser());

                return toDTO(updated);
        }

        // =========================================================
        // 🧩 Helper: Entity → DTO (đệ quy replies)
        // =========================================================
        private CommentDTO toTreeDTO(Comment c) {
                CommentDTO dto = toDTO(c);
                dto.setReplies(
                                c.getReplies().stream()
                                                .map(this::toTreeDTO)
                                                .collect(Collectors.toList()));
                return dto;
        }

        private CommentDTO toDTO(Comment c) {
                return new CommentDTO(
                                c.getCommentId(),
                                c.getTask().getTaskId(),
                                c.getParent() != null ? c.getParent().getCommentId() : null,
                                c.getContent(),
                                c.getUser().getUserId(),
                                c.getUser().getName(),
                                c.getUser().getEmail(),
                                c.getUser().getAvatarUrl(),
                                c.getCreatedAt());
        }

        // =========================================================
        // 🧠 Helper: tự nhận diện mentions trong content
        // =========================================================
        private String detectMentionsFromContent(String content) {
                if (content == null || content.isBlank())
                        return "[]";

                List<Map<String, String>> mentions = new ArrayList<>();

                if (content.contains("@card")) {
                        mentions.add(Map.of("name", "@card", "email", "@card"));
                }
                if (content.contains("@board")) {
                        mentions.add(Map.of("name", "@board", "email", "@board"));
                }

                Matcher m = Pattern.compile("([\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,6})").matcher(content);
                while (m.find()) {
                        String email = m.group(1);
                        mentions.add(Map.of("name", email, "email", email));
                }

                try {
                        return mentions.isEmpty() ? "[]" : mapper.writeValueAsString(mentions);
                } catch (Exception e) {
                        log.warn("⚠️ detectMentionsFromContent() failed: {}", e.getMessage());
                        return "[]";
                }
        }

        // =========================================================
        // 🧩 Helper: parse JSON -> List<CommentDTO> để notifyMentions()
        // =========================================================
        private List<CommentDTO> parseMentions(String mentionsJson) {
                try {
                        if (mentionsJson == null || mentionsJson.isBlank())
                                return List.of();
                        List<Map<String, String>> raw = mapper.readValue(
                                        mentionsJson,
                                        mapper.getTypeFactory().constructCollectionType(List.class, Map.class));

                        return raw.stream()
                                        .map(m -> {
                                                String email = m.get("email");
                                                String name = m.get("name");
                                                return new CommentDTO(null, null, null, null, null, name, email, null,
                                                                null);
                                        })
                                        .toList();
                } catch (Exception e) {
                        log.error("⚠️ parseMentions() failed: {}", e.getMessage());
                        return List.of();
                }
        }

        private String escapeJson(String text) {
                return text == null ? ""
                                : text.replace("\"", "\\\"")
                                                .replace("\n", "\\n")
                                                .replace("\r", "");
        }
}
