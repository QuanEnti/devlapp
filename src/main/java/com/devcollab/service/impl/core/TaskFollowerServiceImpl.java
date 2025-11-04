package com.devcollab.service.impl.core;

import com.devcollab.domain.Task;
import com.devcollab.domain.TaskFollower;
import com.devcollab.domain.User;
import com.devcollab.dto.TaskFollowerDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.TaskFollowerRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.core.TaskFollowerService;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskFollowerServiceImpl implements TaskFollowerService {

    private final TaskFollowerRepository followerRepo;
    private final TaskRepository taskRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;
    private final ActivityService activityService;

    // =========================================================
    // 🔹 Lấy danh sách follower trong 1 task
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public List<TaskFollowerDTO> getFollowersByTask(Long taskId) {
        return followerRepo.findByTask_TaskId(taskId)
                .stream()
                .map(TaskFollowerDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public boolean assignMember(Long taskId, Long userId) {
        if (followerRepo.existsByTask_TaskIdAndUser_UserId(taskId, userId)) {
            log.warn("⚠️ User {} đã được gán vào task {}", userId, taskId);
            return false;
        }

        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new NotFoundException("❌ Task không tồn tại"));
        User addedUser = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("❌ User không tồn tại"));

        followerRepo.saveAndFlush(new TaskFollower(task, addedUser));
        log.info("✅ Đã gán user {} ({}) vào task {}", userId, addedUser.getName(), taskId);

        // 🧾 Xác định actor hiện tại
        User actor = getCurrentActor();

        // 🧾 Ghi activity (actor = người thao tác)
        activityService.log(
                "TASK",
                taskId,
                "ADD_MEMBER",
                "{\"user\":\"" + addedUser.getName() + "\"}",
                actor);

        try {
            if (actor != null && addedUser.getUserId().equals(actor.getUserId())) {
                log.debug("ℹ️ Bỏ qua notify vì actor = target ({})", addedUser.getEmail());
                return true;
            }

            String link = "/projects/" + task.getProject().getProjectId() + "/tasks/" + task.getTaskId();
            String title = "Được thêm vào công việc";
            String message = "đã thêm bạn vào công việc \"" + task.getTitle() + "\"";

            // 🔔 Gửi thông báo cho người vừa được thêm
            notificationService.createNotification(
                    addedUser,
                    "TASK_MEMBER_ADDED",
                    task.getTaskId(),
                    title,
                    message,
                    link,
                    actor);

            // (Tuỳ chọn) Gửi cho follower khác (ngoại trừ actor + addedUser)
            if (task.getFollowers() != null && !task.getFollowers().isEmpty()) {
                for (TaskFollower f : task.getFollowers()) {
                    User follower = f.getUser();
                    if (follower == null)
                        continue;
                    if (actor != null && follower.getUserId().equals(actor.getUserId()))
                        continue;
                    if (follower.getUserId().equals(addedUser.getUserId()))
                        continue;

                    notificationService.createNotification(
                            follower,
                            "TASK_MEMBER_ADDED",
                            task.getTaskId(),
                            "Cập nhật công việc",
                            "đã thêm " + addedUser.getName()
                                    + " vào công việc \"" + task.getTitle() + "\"",
                            link,
                            actor);

                }
            }

            log.info("📩 [Notification] Gửi TASK_MEMBER_ADDED cho {}", addedUser.getEmail());
        } catch (Exception e) {
            log.error("⚠️ Lỗi khi gửi thông báo thêm member: {}", e.getMessage(), e);
        }

        return true;
    }

    @Override
    @Transactional
    public boolean unassignMember(Long taskId, Long userId) {
        if (!followerRepo.existsByTask_TaskIdAndUser_UserId(taskId, userId)) {
            log.warn("⚠️ User {} không được gán trong task {}", userId, taskId);
            return false;
        }

        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new NotFoundException("❌ Task không tồn tại"));
        User removedUser = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("❌ User không tồn tại"));

        followerRepo.deleteByTaskAndUser(taskId, userId);
        log.info("🗑️ Đã bỏ gán user {} ({}) khỏi task {}", userId, removedUser.getName(), taskId);

        // 🧾 Xác định actor
        User actor = getCurrentActor();

        // 🧾 Ghi activity
        activityService.log(
                "TASK",
                taskId,
                "REMOVE_MEMBER",
                "{\"user\":\"" + removedUser.getName() + "\"}",
                actor);

        try {
            if (actor != null && removedUser.getUserId().equals(actor.getUserId())) {
                log.debug("ℹ️ Bỏ qua notify vì actor = target ({})", removedUser.getEmail());
                return true;
            }

            String link = "/projects/" + task.getProject().getProjectId() + "/tasks/" + task.getTaskId();
            String title = "Bị xóa khỏi công việc";
            String message = "đã xóa bạn khỏi công việc \"" + task.getTitle() + "\"";

            notificationService.createNotification(
                    removedUser,
                    "TASK_MEMBER_REMOVED",
                    task.getTaskId(),
                    title,
                    message,
                    link,
                    actor);
            // (Tùy chọn) Gửi cho follower khác (ngoại trừ actor + removedUser)
            if (task.getFollowers() != null && !task.getFollowers().isEmpty()) {
                for (TaskFollower f : task.getFollowers()) {
                    User follower = f.getUser();
                    if (follower == null)
                        continue;
                    if (actor != null && follower.getUserId().equals(actor.getUserId()))
                        continue;
                    if (follower.getUserId().equals(removedUser.getUserId()))
                        continue;

                    notificationService.createNotification(
                            follower,
                            "TASK_MEMBER_REMOVED",
                            task.getTaskId(),
                            "Cập nhật công việc",
                            "đã xóa " + removedUser.getName()
                                    + " khỏi công việc \"" + task.getTitle() + "\"",
                            link,
                            actor);

                }
            }

            log.info("📩 [Notification] Gửi TASK_MEMBER_REMOVED cho {}", removedUser.getEmail());
        } catch (Exception e) {
            log.error("⚠️ Lỗi khi gửi thông báo xóa member: {}", e.getMessage(), e);
        }

        return true;
    }

    /**
     * ✅ Helper: lấy actor hiện tại từ SecurityContext (dùng được cho cả OAuth2 và
     * local login)
     */
    private User getCurrentActor() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated())
                return null;

            String email = null;
            if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidc)
                email = oidc.getEmail();
            else if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User ou)
                email = String.valueOf(ou.getAttributes().get("email"));
            else if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails ud)
                email = ud.getUsername();
            else if (auth.getPrincipal() instanceof String s)
                email = s;

            return (email != null) ? userRepo.findByEmail(email).orElse(null) : null;
        } catch (Exception e) {
            log.error("⚠️ getCurrentActor() failed: {}", e.getMessage());
            return null;
        }
    }

}
