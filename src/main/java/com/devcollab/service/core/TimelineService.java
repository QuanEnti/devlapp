package com.devcollab.service.core;

import com.devcollab.dto.TimelineTaskDTO;
import java.util.List;

public interface TimelineService {
    /**
     * Get all tasks assigned to a user that have a deadline.
     * Includes project name via JOIN with Project.
     * 
     * @param userId The user ID
     * @return List of TimelineTaskDTO with taskId, projectId, projectName, title, deadline, status
     */
    List<TimelineTaskDTO> getTasksForUser(Long userId);
}

