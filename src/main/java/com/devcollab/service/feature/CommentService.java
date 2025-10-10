package com.devcollab.service.feature;

import com.devcollab.domain.Comment;
import java.util.List;

public interface CommentService {

    List<Comment> getCommentsByTask(Long taskId);

    Comment createComment(Long taskId, Long userId, String content);

    Comment updateComment(Long commentId, String content);

    void deleteComment(Long commentId);
}
