package com.devcollab.service.impl.feature;

import com.devcollab.domain.Comment;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.CommentRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.feature.CommentService;
import com.devcollab.service.system.ActivityService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;

    @Override
    public List<Comment> getCommentsByTask(Long taskId) {
        taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        return commentRepository.findByTask_TaskIdOrderByCreatedAtAsc(taskId);
    }

    @Override
    public Comment createComment(Long taskId, Long userId, String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Nội dung bình luận không được để trống");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        Comment comment = new Comment();
        comment.setTask(task);
        comment.setUser(user);
        comment.setContent(content.trim());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setMentionsJson(null);

        Comment saved = commentRepository.save(comment);
        activityService.log("COMMENT", saved.getCommentId(), "CREATE", "by " + user.getName());
        return saved;
    }

    @Override
    public Comment updateComment(Long commentId, String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Nội dung mới không được để trống");
        }

        Comment existing = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bình luận"));

        existing.setContent(content.trim());
        Comment saved = commentRepository.save(existing);

        activityService.log("COMMENT", saved.getCommentId(), "UPDATE", saved.getContent());
        return saved;
    }

    @Override
    public void deleteComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Bình luận không tồn tại");
        }
        commentRepository.deleteById(commentId);
        activityService.log("COMMENT", commentId, "DELETE", "Hard delete");
    }
}
