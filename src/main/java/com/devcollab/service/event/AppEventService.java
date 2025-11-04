package com.devcollab.service.event;

import com.devcollab.domain.Project;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import org.springframework.stereotype.Service;

@Service
public class AppEventService {

    private final NotificationService notificationService;
    private final ActivityService activityService;

    public AppEventService(NotificationService notificationService,
            ActivityService activityService) {
        this.notificationService = notificationService;
        this.activityService = activityService;
    }


    public void publishUserCreated(User user) {
        activityService.log("USER", user.getUserId(), "CREATE", user.getName());
    }

    public void publishUserStatusChanged(User user) {
        activityService.log("USER", user.getUserId(), "STATUS_UPDATE", user.getStatus());
    }

  
    public void publishProjectCreated(Project project) {
        activityService.log("PROJECT", project.getProjectId(), "CREATE", project.getName());
        notificationService.notifyProjectCreated(project);
    }

    public void publishProjectUpdated(Project project) {
        activityService.log("PROJECT", project.getProjectId(), "UPDATE", project.getName());
    }

    public void publishMemberAdded(Project project, User user) {
        activityService.log("PROJECT_MEMBER", project.getProjectId(), "ADD_MEMBER", user.getName());
        notificationService.notifyMemberAdded(project, user);
    }

    public void publishProjectArchived(Project project) {
        activityService.log("PROJECT", project.getProjectId(), "ARCHIVE", project.getName());
        notificationService.notifyProjectArchived(project);
    }



    public void publishTaskUpdated(Task task) {
        activityService.log("TASK", task.getTaskId(), "UPDATE", task.getTitle());
    }

    public void publishTaskMoved(Task task, String fromCol, String toCol) {
        activityService.log("TASK", task.getTaskId(), "MOVE", fromCol + " → " + toCol);
    }

    public void publishTaskReopened(Task task) {
        activityService.log("TASK", task.getTaskId(), "REOPEN", task.getTitle());
    }

    public void publishTaskDeleted(Task task) {
        activityService.log("TASK", task.getTaskId(), "DELETE", task.getTitle());
    }
}
