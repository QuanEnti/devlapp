package com.devcollab.service.system;

import com.devcollab.domain.Notification;
import com.devcollab.domain.Project;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;

import java.util.List;

public interface NotificationService {

    // ======================================================
    // 🔔 Core createNotification (Trello-style)
    // ======================================================
    /**
     * Gửi thông báo đơn giản (giống Trello).
     * 
     * @param receiver Người nhận thông báo
     * @param type     Loại thông báo (ví dụ: TASK_MEMBER_ADDED, PROJECT_CREATED,
     *                 ...)
     * @param message  Nội dung thông báo chính
     * @param link     Liên kết đến trang chi tiết
     */
void createNotification(User receiver, String type, Long refId,
            String title, String message, String link, User sender) ;
    // ======================================================
    // 🗂️ Project-level notifications
    // ======================================================
    void notifyProjectCreated(Project project);

    void notifyMemberAdded(Project project, User user);

    void notifyProjectArchived(Project project);

    // ======================================================
    // 🧩 Task-level notifications
    // ======================================================
    /**
     * Gửi thông báo cho các sự kiện trong Task (assign, comment, due soon...).
     * Nếu có actor (người thực hiện), hệ thống sẽ tự sinh message:
     * "{actor.getName()} đã thêm bạn vào công việc..."
     *
     * @param task      Task liên quan
     * @param actor     Người thực hiện hành động (có thể null → hệ thống)
     * @param eventType Loại sự kiện (TASK_MEMBER_ADDED, TASK_COMMENTED, ...)
     * @param message   Nội dung mô tả (tùy chọn)
     */
    void notifyTaskEvent(Task task, User actor, String eventType, String message);

    /**
     * Gửi thông báo Task cho 1 người cụ thể (ví dụ người được thêm/gỡ).
     */
    void notifyTaskEvent(Task task, User actor, String eventType, String message, User specificReceiver);

    // ======================================================
    // 👤 User-level notifications
    // ======================================================
    void notifyChangeProfile(User user);

    void notifyChangePassword(User user);

    // ======================================================
    // 📩 Common utility methods
    // ======================================================
    int countUnread(String email);

    List<Notification> getNotificationsByUser(String email);

    boolean markAsRead(Long notificationId, String userEmail);

    int markAllAsRead(String userEmail);

    void deleteNotification(Long notificationId);
    void notifyMemberRoleUpdated(Project project, User target, User actor, String newRole);
}
