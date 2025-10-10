package com.devcollab.repository;

import com.devcollab.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByTask_TaskId(Long taskId);
}
