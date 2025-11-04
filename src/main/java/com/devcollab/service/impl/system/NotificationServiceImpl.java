package com.devcollab.service.impl.system;

import com.devcollab.domain.*;
import com.devcollab.repository.NotificationRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import com.devcollab.service.system.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final ActivityService activityService;

    // =========================================================
    // 🔔 Tạo Notification (chuẩn Trello style)
    // =========================================================
    @Transactional
    @Override
    public void createNotification(User receiver, String type, Long refId,
            String title, String message, String link, User sender) {
        if (receiver == null) {
            log.warn("⚠️ [Notification] Receiver is null for type: {}", type);
            return;
        }

        try {
            // ✅ 1. Lưu thông báo vào DB
            Notification notif = new Notification();
            notif.setUser(receiver); // người nhận
            notif.setSender(sender); // người gửi
            notif.setType(type);
            notif.setReferenceId(refId);
            notif.setTitle(title);
            notif.setMessage(message);
            notif.setLink(link);
            notif.setStatus("unread");
            notif.setCreatedAt(LocalDateTime.now());

            notificationRepository.saveAndFlush(notif);

            // ✅ 2. Gửi realtime qua WebSocket (chuẩn Trello)
            webSocketNotificationService.sendToUser(receiver, notif, sender);

            log.info("📨 [Notification] Sent '{}' to {} from {}",
                    type, receiver.getEmail(),
                    sender != null ? sender.getName() : "System");

        } catch (Exception e) {
            log.error("❌ [Notification] Failed to create notification: {}", e.getMessage(), e);
        }
    }

    // =========================================================
    // 🧩 Project-level Notifications
    // =========================================================
    @Override
    @Transactional
    public void notifyProjectCreated(Project project) {
        if (project == null || project.getCreatedBy() == null)
            return;

        createNotification(
                project.getCreatedBy(),
                "PROJECT_CREATED",
                project.getProjectId(),
                "Dự án mới: " + project.getName(),
                "Bạn đã tạo dự án \"" + project.getName() + "\" thành công.",
                "/view/pm/project/board?projectId=" + project.getProjectId(),
                project.getCreatedBy());

    }

    @Override
    @Transactional
    public void notifyMemberAdded(Project project, User user) {
        if (project == null || user == null)
            return;

        createNotification(
                user,
                "MEMBER_ADDED",
                project.getProjectId(),
                "Tham gia dự án: " + project.getName(),
                "đã thêm bạn vào dự án \"" + project.getName() + "\".",
                "/view/pm/project/board?projectId=" + project.getProjectId(),
                project.getCreatedBy());
    }

    @Override
    @Transactional
    public void notifyMemberRoleUpdated(Project project, User target, User actor, String newRole) {
        if (project == null || target == null)
            return;

        try {
            // 🧩 Chuẩn hóa tên vai trò
            String formattedRole = switch (newRole.toUpperCase()) {
                case "OWNER" -> "Chủ dự án";
                case "PM" -> "Quản lý dự án";
                case "ADMIN" -> "Quản trị viên";
                case "MEMBER" -> "Thành viên";
                default -> newRole;
            };

            // 🔁 Đảm bảo entity managed
            User receiver = userRepository.findById(target.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));
            User sender = (actor != null)
                    ? userRepository.findById(actor.getUserId()).orElse(null)
                    : null;

            // 🔗 Tạo thông báo trực tiếp (tránh self-invocation)
            Notification notif = new Notification();
            notif.setUser(receiver);
            notif.setSender(sender);
            notif.setType("PROJECT_MEMBER_ROLE_UPDATED");
            notif.setReferenceId(project.getProjectId());
            notif.setTitle("Cập nhật vai trò thành viên");
            notif.setMessage(" đã chỉ định bạn là " + formattedRole
                    + " của dự án \"" + project.getName() + "\".");
            notif.setLink("/view/pm/project/board?projectId=" + project.getProjectId());

            notif.setStatus("unread");
            notif.setCreatedAt(LocalDateTime.now());

            // 💾 Lưu và flush ngay để đảm bảo có trong DB
            notificationRepository.saveAndFlush(notif);

            // 🔔 Gửi realtime
            webSocketNotificationService.sendToUser(receiver, notif, sender);

            log.info("📨 [Notification] Saved + Sent PROJECT_MEMBER_ROLE_UPDATED to {}", receiver.getEmail());
        } catch (Exception e) {
            log.error("❌ [Notification] Failed to notifyMemberRoleUpdated: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void notifyProjectArchived(Project project) {
        if (project == null || project.getCreatedBy() == null)
            return;

        createNotification(
                project.getCreatedBy(),
                "PROJECT_ARCHIVED",
                project.getProjectId(),
                "Dự án đã được lưu trữ",
                "Dự án \"" + project.getName() + "\" hiện đã được chuyển vào lưu trữ.",
                "/view/pm/project/board?projectId=" + project.getProjectId(),
                project.getCreatedBy());

    }

    // =========================================================
    // 🧩 Task-level Notifications
    // =========================================================
    @Override
    @Transactional
    public void notifyTaskEvent(Task task, User actor, String eventType, String message) {
        notifyTaskEvent(task, actor, eventType, message, null);
    }

    @Override
    @Transactional
    public void notifyTaskEvent(Task task, User actor, String eventType, String message, User specificReceiver) {
        if (task == null) {
            log.warn("⚠️ notifyTaskEvent(): Task is null");
            return;
        }

        List<String> allowedEvents = List.of(
                "TASK_MEMBER_ADDED", "TASK_MEMBER_REMOVED",
                "TASK_COMMENTED", "TASK_DUE_SOON", "TASK_FOLLOWED");

        if (!allowedEvents.contains(eventType)) {
            log.debug("ℹ️ Skipped unsupported event: {}", eventType);
            return;
        }

        try {
            Task managed = taskRepository.findById(task.getTaskId())
                    .orElseThrow(() -> new IllegalArgumentException("Task not found"));
            String link = "/projects/" + managed.getProject().getProjectId()
                    + "/tasks/" + managed.getTaskId();
            String actorName = (actor != null && actor.getName() != null)
                    ? actor.getName()
                    : "Hệ thống";

            if (specificReceiver != null) {
                String msg = buildTaskMessage(eventType, actorName, managed.getTitle(), message, true);
                createNotification(specificReceiver, eventType, managed.getTaskId(),
                        mapTitle(eventType), msg, link, actor);
                return;
            }

            // 👥 Gửi cho tất cả follower trừ người thực hiện
            List<User> receivers = managed.getFollowers().stream()
                    .map(TaskFollower::getUser)
                    .filter(u -> u != null && (actor == null || !u.getUserId().equals(actor.getUserId())))
                    .distinct()
                    .collect(Collectors.toList());

            for (User receiver : receivers) {
                String msg = buildTaskMessage(eventType, actorName, managed.getTitle(), message, false);
                createNotification(receiver, eventType, managed.getTaskId(),
                        mapTitle(eventType), msg, link, actor);
            }

            log.info("✅ [Notification] Sent '{}' to {} follower(s)", eventType, receivers.size());

        } catch (Exception e) {
            log.error("❌ notifyTaskEvent() failed: {}", e.getMessage(), e);
        }
    }

    private String buildTaskMessage(String eventType, String actorName, String taskTitle, String custom,
            boolean direct) {
        return switch (eventType) {
            case "TASK_MEMBER_ADDED" ->
                direct ? actorName + " đã thêm bạn vào công việc \"" + taskTitle + "\""
                        : actorName + " đã thêm thành viên vào \"" + taskTitle + "\"";
            case "TASK_MEMBER_REMOVED" ->
                direct ? actorName + " đã xóa bạn khỏi công việc \"" + taskTitle + "\""
                        : actorName + " đã xóa một thành viên khỏi \"" + taskTitle + "\"";
            case "TASK_COMMENTED" ->
                actorName + " đã bình luận: \"" + custom + "\" trong \"" + taskTitle + "\"";
            case "TASK_DUE_SOON" ->
                "⏰ Công việc \"" + taskTitle + "\" sắp đến hạn!";
            case "TASK_FOLLOWED" ->
                actorName + " đang theo dõi công việc \"" + taskTitle + "\"";
            default -> "Công việc \"" + taskTitle + "\" có cập nhật mới.";
        };
    }

    // =========================================================
    // 👤 User-level Notifications
    // =========================================================
    @Override
    @Transactional
    public void notifyChangeProfile(User user) {
        if (user == null)
            return;
        createNotification(user, "PROFILE_UPDATED", user.getUserId(),
                "Cập nhật hồ sơ", "Thông tin tài khoản của bạn đã được thay đổi.",
                "/profile", user);
    }

    @Override
    @Transactional
    public void notifyChangePassword(User user) {
        if (user == null)
            return;
        createNotification(user, "PASSWORD_CHANGED", user.getUserId(),
                "Đổi mật khẩu", "Mật khẩu tài khoản của bạn đã được cập nhật thành công.",
                "/security", user);
    }

    // =========================================================
    // 📊 Utility Methods
    // =========================================================
    @Override
    public int countUnread(String email) {
        return userRepository.findByEmail(email)
                .map(u -> notificationRepository.countUnreadByUserId(u.getUserId()))
                .orElse(0);
    }

    @Override
    public List<Notification> getNotificationsByUser(String email) {
        return userRepository.findByEmail(email)
                .map(u -> notificationRepository.findNotificationsByUserId(u.getUserId()))
                .orElse(List.of());
    }

    @Override
    @Transactional
    public boolean markAsRead(Long id, String email) {
        return notificationRepository.findById(id)
                .map(n -> {
                    if (n.getUser() == null || n.getUser().getEmail() == null)
                        return false;
                    if (!n.getUser().getEmail().equalsIgnoreCase(email))
                        return false;

                    if (!"read".equalsIgnoreCase(n.getStatus())) {
                        n.setStatus("read");
                        n.setReadAt(LocalDateTime.now());
                        notificationRepository.save(n);
                        log.info("✅ [Notification] Marked {} as read", id);
                    }
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public int markAllAsRead(String email) {
        return userRepository.findByEmail(email)
                .map(u -> notificationRepository.markAllAsReadByUserId(u.getUserId()))
                .orElse(0);
    }

    @Override
    @Transactional
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
        log.info("🗑️ [Notification] Deleted id={}", id);
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


    // =========================================================
    // 🧭 Mapping helpers
    // =========================================================
    private static final Map<String, String> TITLE_MAP = Map.ofEntries(
            Map.entry("PROJECT_CREATED", "Dự án mới"),
            Map.entry("PROJECT_ARCHIVED", "Dự án đã được lưu trữ"),
            Map.entry("MEMBER_ADDED", "Được thêm vào dự án"),
            Map.entry("PROJECT_MEMBER_ROLE_UPDATED", "Cập nhật vai trò thành viên"),
            Map.entry("TASK_MEMBER_ADDED", "Được thêm vào công việc"),
            Map.entry("TASK_MEMBER_REMOVED", "Bị xóa khỏi công việc"),
            Map.entry("TASK_COMMENTED", "Bình luận mới"),
            Map.entry("TASK_DUE_SOON", "Công việc sắp đến hạn"),
            Map.entry("TASK_FOLLOWED", "Công việc được theo dõi"),
            Map.entry("PROFILE_UPDATED", "Cập nhật hồ sơ"),
            Map.entry("PASSWORD_CHANGED", "Đổi mật khẩu"));

    private static final Map<String, String> ICON_MAP = Map.ofEntries(
            Map.entry("PROJECT_CREATED", "🗂️"),
            Map.entry("PROJECT_MEMBER_ROLE_UPDATED", "👤"),
            Map.entry("MEMBER_ADDED", "👥"),
            Map.entry("TASK_MEMBER_ADDED", "👤"),

            Map.entry("TASK_COMMENTED", "💬"),
            Map.entry("TASK_DUE_SOON", "⏰"),
            Map.entry("TASK_FOLLOWED", "⭐"),
            Map.entry("PASSWORD_CHANGED", "🔑"),
            Map.entry("PROFILE_UPDATED", "⚙️"));

    private String mapTitle(String type) {
        return TITLE_MAP.getOrDefault(type, "Thông báo mới");
    }

    private String mapIcon(String type) {
        return ICON_MAP.getOrDefault(type, "📢");
    }
    
}