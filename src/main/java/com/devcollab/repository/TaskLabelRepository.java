package com.devcollab.repository;

import com.devcollab.domain.TaskLabel;
import com.devcollab.domain.TaskLabelId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskLabelRepository extends JpaRepository<TaskLabel, TaskLabelId> {
    List<TaskLabel> findByTask_TaskId(Long taskId);
}
