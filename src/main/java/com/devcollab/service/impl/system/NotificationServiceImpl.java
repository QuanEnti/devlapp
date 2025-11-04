package com.devcollab.service.impl.system;

import com.devcollab.domain.*;
import com.devcollab.repository.NotificationRepository;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

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

    @Override
    public void notifyChangeProfile(User user) { // Khi người dùng thay đổi hồ sơ sẽ tạo thông báo nhưng sẽ bị lỗi (Hàm này không cần thiết lắm)
        Notification n = new Notification();
        n.setUser(user);
        n.setType("PROFILE_UPDATED");
        n.setReferenceId(null); // lỗi ở đây vì null referenceId
        n.setStatus("unread");
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);

        activityService.log("USER", user.getUserId(), "NOTIFY_CHANGE_PROFILE",
                "Hồ sơ người dùng đã được cập nhật: " + user.getName());
    }

    @Override
    public void notifyChangePassword(User user) {// Khi người dùng thay đổi mật khẩu sẽ tạo thông báo nhưng sẽ bị lỗi (Hàm này không cần thiết lắm)
        Notification n = new Notification();
        n.setUser(user);
        n.setType("PASSWORD_CHANGED");
        n.setReferenceId(null);// lỗi ở đây vì null referenceId
        n.setStatus("unread");
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);

        activityService.log("USER", user.getUserId(), "NOTIFY_CHANGE_PASSWORD",
                "Người dùng đã đổi mật khẩu: " + user.getEmail());
    }

    @Override
    public void notifyPaymentSuccess(User user, PaymentOrder order) {
        if (user == null || order == null) return;

        Notification n = new Notification();
        n.setUser(user);
        n.setType("PAYMENT_SUCCESS");
        n.setReferenceId(order.getId());
        n.setStatus("unread");
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);

        // 🧾 Ghi log hoạt động
        activityService.log("PAYMENT", order.getId(), "NOTIFY_PAYMENT_SUCCESS",
                "Thanh toán thành công cho đơn hàng: " + order.getName());

        System.out.println("📢 Đã tạo thông báo thanh toán thành công cho " + user.getEmail());
    }

    @Override
    public int countUnread(String username) {
        return notificationRepository.countUnreadByUserEmail(username);
    }

    @Override
    public List<Notification> getNotificationsByUser(String email) {
        return notificationRepository.findNotificationsByUserEmail(email);
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setStatus("read");
            notificationRepository.save(n);
        });
    }

    @Override
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}
