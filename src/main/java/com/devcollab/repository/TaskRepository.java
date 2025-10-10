package com.devcollab.repository;

import com.devcollab.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProject_ProjectId(Long projectId);

    List<Task> findByAssignee_UserId(Long userId);

    List<Task> findByStatus(String status);
    
    List<Task> findByLabels_LabelId(Long labelId);
}
