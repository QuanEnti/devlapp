package com.devcollab.repository;

import com.devcollab.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTask_TaskIdAndParentIsNullOrderByCreatedAtDesc(Long taskId);

    List<Comment> findByParent_CommentIdOrderByCreatedAtAsc(Long parentId);

    long countByTask_TaskIdAndParentIsNull(Long taskId);

    @Query("SELECT c.task.taskId, COUNT(c) FROM Comment c WHERE c.task.taskId IN :taskIds AND c.parent IS NULL GROUP BY c.task.taskId")
    List<Object[]> countByTaskIds(@Param("taskIds") List<Long> taskIds);
}