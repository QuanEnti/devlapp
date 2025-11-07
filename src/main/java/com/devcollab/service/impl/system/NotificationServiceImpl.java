package com.devcollab.service.impl.system;

import com.devcollab.domain.*;
import com.devcollab.dto.CommentDTO;
import com.devcollab.repository.NotificationRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.MailService;
import com.devcollab.service.system.NotificationService;
import com.devcollab.service.system.UserSettingsService;
import com.devcollab.service.system.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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
    private final MailService mailService;
    private final UserSettingsService userSettingsService;


    @Transactional
    @Override
    public void createNotification(User receiver, String type, Long refId,
            String title, String message, String link, User sender) {
        if (receiver == null) {
            log.warn("⚠️ [Notification] Receiver is null for type: {}", type);
            return;
        }

        try {
            // 🧩 Tạo đối tượng notification
            Notification notif = new Notification();
            notif.setUser(receiver);
            notif.setSender(sender);
            notif.setType(type);
            notif.setReferenceId(refId);
            notif.setTitle(title);
            notif.setMessage(message);
            notif.setLink(link);
            notif.setStatus("unread");
            notif.setCreatedAt(LocalDateTime.now());

            // 🔎 Xác định mức độ ưu tiên (HIGH / MEDIUM / LOW)
            String priority = determinePriority(type);
            notif.setPriority(priority);
            notif.setEmailed(false);

            notificationRepository.saveAndFlush(notif);


            sendRealtime(receiver, notif, sender);


            UserSettings settings = userSettingsService.getOrDefault(receiver);

            if (!settings.isEmailEnabled()) {
                log.info("🚫 [Notification] Email disabled for user {}", receiver.getEmail());
                return;
            }

            if ("HIGH".equalsIgnoreCase(priority)) {

                if (settings.isEmailHighImmediate()) {
                    sendEmail(notif, receiver, title, message, link, sender);
                    notif.setEmailed(true);
                    notificationRepository.save(notif);
                    log.info("📨 Sent HIGH email immediately to {}", receiver.getEmail());
                } else {
                    log.info("⏳ Queued HIGH into digest for {}", receiver.getEmail());
                }

            } else if ("MEDIUM".equalsIgnoreCase(priority)) {

                if (settings.isEmailDigestEnabled()) {
                    log.info("⏳ Queued MEDIUM into digest for {}", receiver.getEmail());
                } else {
                    log.info("🚫 MEDIUM digest disabled for {}", receiver.getEmail());
                }

            } else {
                log.info("💬 LOW (Realtime only) for {}", receiver.getEmail());
            }

            log.info("✅ [Notification] Created '{}' for {} from {}",
                    type, receiver.getEmail(),
                    sender != null ? sender.getName() : "System");

        } catch (Exception e) {
            log.error("❌ [Notification] Failed to create notification: {}", e.getMessage(), e);
        }
    }

    private void sendEmail(Notification notif, User receiver,
            String title, String message, String link, User sender) {
        if (receiver.getEmail() == null || receiver.getEmail().isBlank()) {
            log.warn("⚠️ [Notification] No email address found for {}", receiver.getUserId());
            return;
        }

        mailService.sendNotificationMail(
                receiver.getEmail(),
                title,
                message,
                link,
                sender != null ? sender.getName() : "DevCollab System");
    }

    @Async
    protected void sendRealtime(User receiver, Notification notif, User sender) {
        try {
            webSocketNotificationService.sendToUser(receiver, notif, sender);
        } catch (Exception e) {
            log.warn("⚠️ Realtime push failed: {}", e.getMessage());
        }
    }

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
            String formattedRole = switch (newRole.toUpperCase()) {
                case "OWNER" -> "Chủ dự án";
                case "PM" -> "Quản lý dự án";
                case "ADMIN" -> "Quản trị viên";
                case "MEMBER" -> "Thành viên";
                default -> newRole;
            };

            User receiver = userRepository.findById(target.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));
            User sender = (actor != null)
                    ? userRepository.findById(actor.getUserId()).orElse(null)
                    : null;

            createNotification(
                    receiver,
                    "PROJECT_MEMBER_ROLE_UPDATED",
                    project.getProjectId(),
                    "Cập nhật vai trò thành viên",
                    "đã chỉ định bạn là " + formattedRole +
                            " của dự án \"" + project.getName() + "\".",
                    "/view/pm/project/board?projectId=" + project.getProjectId(),
                    sender);

            log.info("📨 [Notification] Sent PROJECT_MEMBER_ROLE_UPDATED to {}", receiver.getEmail());
        } catch (Exception e) {
            log.error("❌ notifyMemberRoleUpdated(): {}", e.getMessage(), e);
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

        if (!allowedEvents.contains(eventType))
            return;

        try {
            Task managed = taskRepository.findById(task.getTaskId())
                    .orElseThrow(() -> new IllegalArgumentException("Task not found"));
            String link = "/projects/" + managed.getProject().getProjectId()
                    + "/tasks/" + managed.getTaskId();
            String actorName = (actor != null && actor.getName() != null) ? actor.getName() : "Hệ thống";

            if (specificReceiver != null) {
                String msg = buildTaskMessage(eventType, actorName, managed.getTitle(), message, true);
                createNotification(specificReceiver, eventType, managed.getTaskId(),
                        mapTitle(eventType), msg, link, actor);
                return;
            }

            List<User> receivers = managed.getFollowers().stream()
                    .map(TaskFollower::getUser)
                    .filter(u -> u != null && (actor == null || !u.getUserId().equals(actor.getUserId())))
                    .distinct().toList();

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
            case "TASK_MEMBER_ADDED" -> direct
                    ? actorName + " đã thêm bạn vào công việc \"" + taskTitle + "\""
                    : actorName + " đã thêm thành viên vào \"" + taskTitle + "\"";
            case "TASK_MEMBER_REMOVED" -> direct
                    ? actorName + " đã xóa bạn khỏi công việc \"" + taskTitle + "\""
                    : actorName + " đã xóa một thành viên khỏi \"" + taskTitle + "\"";
            case "TASK_COMMENTED" -> actorName + " đã bình luận: \"" + custom + "\" trong \"" + taskTitle + "\"";
            case "TASK_DUE_SOON" -> "⏰ Công việc \"" + taskTitle + "\" sắp đến hạn!";
            case "TASK_FOLLOWED" -> actorName + " đang theo dõi công việc \"" + taskTitle + "\"";
            default -> "Công việc \"" + taskTitle + "\" có cập nhật mới.";
        };
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyMentions(Task task, User actor, List<CommentDTO> mentions) {
        if (task == null || mentions == null || mentions.isEmpty()) {
            log.debug("⚠️ [Notification] Skip notifyMentions() — no mentions or invalid task");
            return;
        }

        try {
            Project project = task.getProject();
            String actorName = (actor != null && actor.getName() != null) ? actor.getName() : "Hệ thống";
            String taskLink = "/view/pm/task/detail?taskId=" + task.getTaskId();
            String projectLink = "/view/pm/project/board?projectId=" + project.getProjectId();

            List<String> emails = mentions.stream()
                    .map(CommentDTO::getUserEmail)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();

            log.info("💬 [Mention] Processing mentions for task {}: {}", task.getTaskId(), emails);

            for (String email : emails) {

                if ("@card".equalsIgnoreCase(email)) {
                    Set<User> cardMembers = new HashSet<>();
                    if (task.getAssignee() != null)
                        cardMembers.add(task.getAssignee());
                    if (task.getCreatedBy() != null)
                        cardMembers.add(task.getCreatedBy());
                    if (task.getFollowers() != null) {
                        task.getFollowers().forEach(f -> {
                            if (f.getUser() != null)
                                cardMembers.add(f.getUser());
                        });
                    }

                    cardMembers.removeIf(u -> actor != null && u.getUserId().equals(actor.getUserId()));

                    for (User receiver : cardMembers) {
                        createNotification(receiver, "TASK_COMMENT_MENTION", task.getTaskId(),
                                "Nhắc đến trong thẻ",
                                " đã nhắc đến bạn trong thẻ \"" + task.getTitle() + "\".",
                                taskLink, actor);
                    }

                    log.info("📨 [Mention] Sent @card to {} member(s)", cardMembers.size());
                    continue;
                }

                if ("@board".equalsIgnoreCase(email)) {
                    Set<User> boardMembers = project.getMembers().stream()
                            .map(ProjectMember::getUser)
                            .filter(Objects::nonNull)
                            .filter(u -> actor == null || !u.getUserId().equals(actor.getUserId()))
                            .collect(Collectors.toSet());

                    for (User receiver : boardMembers) {
                        createNotification(receiver, "PROJECT_COMMENT_MENTION", project.getProjectId(),
                                "Nhắc đến trong bảng dự án",
                                " đã nhắc đến bạn trong dự án \"" + project.getName() + "\".",
                                projectLink, actor);
                    }

                    log.info("📨 [Mention] Sent @board to {} member(s)", boardMembers.size());
                    continue;
                }

                userRepository.findByEmail(email).ifPresentOrElse(receiver -> {
                    if (actor != null && receiver.getUserId().equals(actor.getUserId()))
                        return;

                    createNotification(receiver, "TASK_COMMENT_MENTION", task.getTaskId(),
                            "Bạn được nhắc đến",
                            " đã nhắc đến bạn trong thẻ \"" + task.getTitle() + "\".",
                            taskLink, actor);
                    log.info("📨 [Mention] Sent direct mention to {}", email);

                }, () -> log.debug("⚠️ [Mention] Skipped unknown email: {}", email));
            }

            activityService.log(
                    "MENTION",
                    task.getTaskId(),
                    "NOTIFY_MENTIONS",
                    "Đã gửi thông báo mention cho " + emails.size() + " mục.");

        } catch (Exception e) {
            log.error("❌ [Notification] notifyMentions() failed: {}", e.getMessage(), e);
        }
    }

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
    
    @Override
    public void notifyPaymentSuccess(User user, PaymentOrder order) {
        if (user == null || order == null)
            return;

        Notification n = new Notification();
        n.setUser(user);
        n.setType("PAYMENT_SUCCESS");
        n.setReferenceId(order.getId());
        n.setStatus("unread");
        n.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(n);

        activityService.log("PAYMENT", order.getId(), "NOTIFY_PAYMENT_SUCCESS",
                "Thanh toán thành công cho đơn hàng: " + order.getName());

        System.out.println("📢 Đã tạo thông báo thanh toán thành công cho " + user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public int countUnread(String email) {
        return userRepository.findByEmail(email)
                .map(u -> notificationRepository.countUnreadByUserId(u.getUserId()))
                .orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
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
    }
    
    private String determinePriority(String type) {
        if (type == null)
            return "LOW";

        return switch (type.toUpperCase()) {
            case "TASK_COMMENT_MENTION", "PROJECT_COMMENT_MENTION",
                    "MEMBER_ADDED", "TASK_MEMBER_ADDED",
                    "PROJECT_MEMBER_ROLE_UPDATED",
                    "PASSWORD_CHANGED", "PAYMENT_SUCCESS",
                    "TASK_DUE_SOON" -> 
                "HIGH";
            case "TASK_COMMENTED", "TASK_MEMBER_REMOVED",
                    "PROJECT_CREATED", "PROJECT_ARCHIVED",
                    "TASK_FOLLOWED" ->
                "MEDIUM";
            default -> "LOW";
        };
    }

    private static final Map<String, String> TITLE_MAP = Map.ofEntries(
            Map.entry("PROJECT_CREATED", "Dự án mới"),
            Map.entry("PROJECT_ARCHIVED", "Dự án đã được lưu trữ"),
            Map.entry("MEMBER_ADDED", "Được thêm vào dự án"),
            Map.entry("PROJECT_MEMBER_ROLE_UPDATED", "Cập nhật vai trò thành viên"),
            Map.entry("TASK_MEMBER_ADDED", "Được thêm vào công việc"),
            Map.entry("TASK_MEMBER_REMOVED", "Bị xóa khỏi công việc"),
            Map.entry("TASK_COMMENTED", "Bình luận mới"),
            Map.entry("TASK_COMMENT_MENTION", "Bạn được nhắc đến"),
            Map.entry("PROJECT_COMMENT_MENTION", "Bạn được nhắc đến trong dự án"),
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
            Map.entry("TASK_COMMENT_MENTION", "📣"),
            Map.entry("PROJECT_COMMENT_MENTION", "📢"),
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
