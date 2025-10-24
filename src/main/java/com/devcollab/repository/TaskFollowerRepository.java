package com.devcollab.repository;

import com.devcollab.domain.TaskFollower;
import com.devcollab.domain.TaskFollowerId;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TaskFollowerRepository extends JpaRepository<TaskFollower, TaskFollowerId> {

    List<TaskFollower> findByTask_TaskId(Long taskId);

    @Query("SELECT CASE WHEN COUNT(tf) > 0 THEN true ELSE false END FROM TaskFollower tf " +
            "WHERE tf.task.taskId = :taskId AND tf.user.userId = :userId")
    boolean existsByTaskAndUser(@Param("taskId") Long taskId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM TaskFollower tf WHERE tf.task.taskId = :taskId AND tf.user.userId = :userId")
    void deleteByTaskAndUser(@Param("taskId") Long taskId, @Param("userId") Long userId);
}
