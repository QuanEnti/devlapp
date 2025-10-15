package com.devcollab.service.core;

import com.devcollab.domain.Task;
import java.util.List;

public interface TaskService {

    Task createTask(Task task, Long creatorId);

    Task updateTask(Long id, Task patch);

    Task assignTask(Long taskId, Long assigneeId);

    Task moveTask(Long taskId, Long columnId);

    Task closeTask(Long taskId);

    Task reopenTask(Long taskId);

    void deleteTask(Long id);

    List<Task> getTasksByProject(Long projectId);

    List<Task> getTasksByAssignee(Long userId);
    List<Task> getTasksByProjectAndMember(Long projectId, String email);
}
