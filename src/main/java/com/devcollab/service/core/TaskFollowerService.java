package com.devcollab.service.core;

import com.devcollab.dto.TaskFollowerDTO;
import java.util.List;

public interface TaskFollowerService {
    List<TaskFollowerDTO> getFollowersByTask(Long taskId);

    boolean assignMember(Long taskId, Long userId);

    boolean unassignMember(Long taskId, Long userId);
}
