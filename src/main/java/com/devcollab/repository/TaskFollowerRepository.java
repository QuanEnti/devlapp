package com.devcollab.repository;

import com.devcollab.domain.TaskFollower;
import com.devcollab.domain.TaskFollowerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskFollowerRepository extends JpaRepository<TaskFollower, TaskFollowerId> {

    List<TaskFollower> findByTask_TaskId(Long taskId);

    boolean existsByTask_TaskIdAndUser_UserId(Long taskId, Long userId);

    @Modifying
    @Query("DELETE FROM TaskFollower f WHERE f.task.taskId = :taskId AND f.user.userId = :userId")
    void deleteByTaskAndUser(@Param("taskId") Long taskId, @Param("userId") Long userId);
}
