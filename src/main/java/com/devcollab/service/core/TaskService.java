package com.devcollab.service.core;

import com.devcollab.domain.ProjectMember;
import com.devcollab.domain.Task;
import com.devcollab.dto.TaskDTO;
import com.devcollab.dto.request.MoveTaskRequest;
import com.devcollab.dto.request.TaskDatesUpdateReq;

import java.util.List;

public interface TaskService {


    Task createTask(Task task, Long creatorId);

    Task updateTask(Long id, Task patch);

    void deleteTask(Long id);

    Task createTaskFromDTO(TaskDTO dto, Long creatorId);

    Task assignTask(Long taskId, Long assigneeId);

    Task moveTask(Long taskId, Long columnId);

    Task closeTask(Long taskId);

    Task reopenTask(Long taskId);

    List<TaskDTO> getTasksByProject(Long projectId);

    List<Task> getTasksByAssignee(Long userId);

    List<Task> getTasksByProjectAndMember(Long projectId, String email);

    Task quickCreate(String title, Long columnId, Long projectId, Long creatorId);
    
    Task getById(Long id);

    Task updateTaskDescription(Long id, String description);

    TaskDTO updateDates(Long taskId, TaskDTO dto);

    TaskDTO moveTask(Long taskId, MoveTaskRequest req);
    
    TaskDTO getByIdAsDTO(Long id);
}
