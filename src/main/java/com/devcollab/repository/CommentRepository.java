package com.devcollab.repository;

import com.devcollab.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTask_TaskIdOrderByCreatedAtAsc(Long taskId);
}
