package com.devcollab.repository;

import com.devcollab.domain.TaskLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskLinkRepository extends JpaRepository<TaskLink, Long> {
    List<TaskLink> findByFromTask_TaskId(Long taskId);
}
