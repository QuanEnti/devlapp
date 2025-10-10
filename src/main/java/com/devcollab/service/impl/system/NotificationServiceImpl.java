package com.devcollab.service.impl.system;

import com.devcollab.domain.Notification;
import com.devcollab.domain.Project;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.repository.NotificationRepository;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ActivityService activityService;

    @Override
    public void notifyProjectCreated(Project project) {
        Notification n = new Notification();
        n.setUser(project.getCreatedBy());
        n.setType("PROJECT_CREATED");
        n.setReferenceId(project.getProjectId());
        n.setStatus("unread");
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);

        activityService.log("PROJECT", project.getProjectId(), "NOTIFY_CREATE",
                "Thông báo tạo dự án: " + project.getName());
    }

    @Override
    public void notifyMemberAdded(Project project, User user) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType("MEMBER_ADDED");
        n.setReferenceId(project.getProjectId());
        n.setStatus("unread");
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);

        activityService.log("PROJECT_MEMBER", project.getProjectId(), "NOTIFY_ADD_MEMBER",
                "Thêm thành viên: " + user.getName());
    }

    @Override
    public void notifyProjectArchived(Project project) {
        Notification n = new Notification();
        n.setUser(project.getCreatedBy());
        n.setType("PROJECT_ARCHIVED");
        n.setReferenceId(project.getProjectId());
        n.setStatus("unread");
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);

        activityService.log("PROJECT", project.getProjectId(), "NOTIFY_ARCHIVE",
                "Dự án đã được lưu trữ: " + project.getName());
    }

    @Override
    public void notifyTaskAssigned(Task task) {
        if (task.getAssignee() == null)
            return;

        Notification n = new Notification();
        n.setUser(task.getAssignee());
        n.setType("TASK_ASSIGNED");
        n.setReferenceId(task.getTaskId());
        n.setStatus("unread");
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);

        activityService.log("TASK", task.getTaskId(), "NOTIFY_ASSIGN",
                "Task được giao cho: " + task.getAssignee().getName());
    }

    @Override
    public void notifyTaskClosed(Task task) {
        if (task.getAssignee() == null)
            return;

        Notification n = new Notification();
        n.setUser(task.getAssignee());
        n.setType("TASK_CLOSED");
        n.setReferenceId(task.getTaskId());
        n.setStatus("unread");
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);

        activityService.log("TASK", task.getTaskId(), "NOTIFY_CLOSE",
                "Task đã được đóng: " + task.getTitle());
    }
}
