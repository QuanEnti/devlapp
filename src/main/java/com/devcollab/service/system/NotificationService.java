package com.devcollab.service.system;

import com.devcollab.domain.Project;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;

public interface NotificationService {

    void notifyProjectCreated(Project project);

    void notifyMemberAdded(Project project, User user);

    void notifyProjectArchived(Project project);

    void notifyTaskAssigned(Task task);

    void notifyTaskClosed(Task task);
}
