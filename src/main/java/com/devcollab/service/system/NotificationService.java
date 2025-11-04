package com.devcollab.service.system;

import com.devcollab.domain.*;

import java.util.List;

public interface NotificationService {

    void notifyProjectCreated(Project project);

    void notifyMemberAdded(Project project, User user);

    void notifyProjectArchived(Project project);

    void notifyTaskAssigned(Task task);

    void notifyTaskClosed(Task task);

    void notifyChangeProfile(User user);

    void notifyChangePassword(User user);

    int countUnread(String username);

    // 🔹 Lấy danh sách thông báo theo user
    List<Notification> getNotificationsByUser(String email);

    // 🔹 Đánh dấu đã đọc
    void markAsRead(Long notificationId);

    // 🔹 Xóa thông báo
    void deleteNotification(Long notificationId);

    void notifyPaymentSuccess(User user, PaymentOrder order);

}
