package com.devcollab.repository;

import com.devcollab.domain.TaskFollower;
import com.devcollab.domain.TaskFollowerId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskFollowerRepository extends JpaRepository<TaskFollower, TaskFollowerId> {
    List<TaskFollower> findByTask_TaskId(Long taskId);

    List<TaskFollower> findByUser_UserId(Long userId);
}
