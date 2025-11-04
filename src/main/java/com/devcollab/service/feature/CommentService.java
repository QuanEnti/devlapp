package com.devcollab.service.feature;

import com.devcollab.dto.CommentDTO;
import java.util.List;

public interface CommentService {

    /**
     * 🟩 Thêm comment gốc (root comment) cho task
     */
    CommentDTO addComment(Long taskId, Long userId, String content, String mentionsJson);

    /**
     * 🔁 Trả lời (reply) tới 1 comment cha
     */
    CommentDTO replyToComment(Long parentId, Long userId, String content);

    /**
     * 🧾 Lấy toàn bộ comment (bao gồm replies) của 1 task
     */
    List<CommentDTO> getCommentsByTask(Long taskId);

    /**
     * 🗑️ Xóa comment (chỉ chủ sở hữu mới được phép)
     */
    void deleteComment(Long commentId, Long userId);

    CommentDTO updateComment(Long commentId, Long userId, String newContent);

}
