package com.devcollab.service.core;

import com.devcollab.domain.ProjectMember;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.dto.MemberPerformanceDTO;
import com.devcollab.dto.TaskDTO;
import com.devcollab.dto.request.MoveTaskRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface TaskService {

    Task createTask(Task task, Long creatorId);

    Task createTaskFromDTO(TaskDTO dto, Long creatorId);

    Task updateTask(Long id, Task patch);

    TaskDTO updateTaskDescription(Long id, String description);

    TaskDTO updateDates(Long taskId, TaskDTO dto);

    void deleteTask(Long id, User actor);

    Task quickCreate(String title, Long columnId, Long projectId, Long creatorId);

    Task getById(Long id);

    TaskDTO getByIdAsDTO(Long id);

    List<TaskDTO> getTasksByProject(Long projectId);

    List<Task> getTasksByAssignee(Long userId);

    List<Task> getTasksByProjectAndMember(Long projectId, String email);

    Task assignTask(Long taskId, Long assigneeId);

    Task closeTask(Long taskId);

    Task reopenTask(Long taskId);

    TaskDTO markComplete(Long taskId, Long userId);

    TaskDTO moveTask(Long taskId, MoveTaskRequest req);

    boolean archiveTask(Long taskId, User actor);

    boolean restoreTask(Long taskId, User actor);

    void removeDeadline(Long taskId, User actor);

    List<Task> getTasksByAssignee(User user);

    List<Task> getTasksCreatedBy(User user);

    List<Task> getTasksByUser(User user);

    Map<String, Object> getPercentDoneByStatus(Long projectId);

    List<MemberPerformanceDTO> getMemberPerformance(Long projectId);

    Page<Task> getUserTasksPaged(User user, String sortBy, int page, int size, String status);

    List<Task> findUpcomingDeadlines(Long userId);
}
