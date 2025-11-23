package com.devcollab.service.impl.core;

import com.devcollab.domain.Task;
import com.devcollab.dto.TimelineTaskDTO;
import com.devcollab.repository.TaskRepository;
import com.devcollab.service.core.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineServiceImpl implements TimelineService {

    private final TaskRepository taskRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TimelineTaskDTO> getTasksForUser(Long userId) {
        List<Task> tasks = taskRepository.findTasksWithDeadlineByAssignee(userId);
        
        return tasks.stream()
                .map(task -> new TimelineTaskDTO(
                    task.getTaskId(),
                    task.getProject() != null ? task.getProject().getProjectId() : null,
                    task.getProject() != null ? task.getProject().getName() : "",
                    task.getTitle(),
                    task.getDeadline() != null 
                            ? task.getDeadline().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            : "",
                    task.getStatus() != null ? task.getStatus() : ""
                ))
                .collect(Collectors.toList());
    }
}

