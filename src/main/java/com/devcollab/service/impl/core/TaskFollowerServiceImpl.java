package com.devcollab.service.impl.core;

import com.devcollab.config.SpringContext;
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
import com.devcollab.service.system.ProjectAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class    TaskFollowerServiceImpl implements TaskFollowerService {

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
        return followerRepo.findByTask_TaskId(taskId).stream().map(TaskFollowerDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public boolean assignMember(Long taskId, Long userId) {
        // 🔍 1️⃣ Lấy thông tin Task và Project liên quan
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new NotFoundException("❌ Task không tồn tại"));
        Long projectId = task.getProject().getProjectId();

        // 👤 2️⃣ Lấy actor hiện tại (người đang thao tác)
        User actor = getCurrentActor();
        if (actor == null)
            throw new AccessDeniedException(
                    "Bạn chưa đăng nhập hoặc không xác định được tài khoản!");

        String email = actor.getEmail();

        // 🔐 3️⃣ Chỉ PM hoặc ADMIN của dự án mới được gán thành viên
        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);
        authz.ensurePmOfProject(email, projectId);

        // 🧭 4️⃣ Kiểm tra nếu user đã được gán trong task rồi
        if (followerRepo.existsByTask_TaskIdAndUser_UserId(taskId, userId)) {
            log.warn("⚠️ User {} đã được gán vào task {}", userId, taskId);
            return false;
        }

        // 🧩 5️⃣ Lấy user được thêm
        User addedUser = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("❌ User không tồn tại"));

        // 💾 6️⃣ Gán user vào task
        followerRepo.saveAndFlush(new TaskFollower(task, addedUser));
        log.info("✅ Đã gán user {} ({}) vào task {}", userId, addedUser.getName(), taskId);

        // 🪶 7️⃣ Ghi activity
        activityService.log("TASK", taskId, "ADD_MEMBER",
                "{\"user\":\"" + addedUser.getName() + "\"}", actor);

        // 🔔 8️⃣ Gửi thông báo (bỏ qua nếu actor = target)
        try {
            if (!addedUser.getUserId().equals(actor.getUserId())) {
                String link = "/projects/" + projectId + "/tasks/" + task.getTaskId();
                String title = "Được thêm vào công việc";
                String message = "đã thêm bạn vào công việc \"" + task.getTitle() + "\"";
                notificationService.createNotification(addedUser, "TASK_MEMBER_ADDED",
                        task.getTaskId(), title, message, link, actor);
            }
        } catch (Exception e) {
            log.error("⚠️ Lỗi khi gửi thông báo thêm member: {}", e.getMessage(), e);
        }

        return true;
    }


    @Override
    @Transactional
    public boolean unassignMember(Long taskId, Long userId) {
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new NotFoundException("❌ Task không tồn tại"));
        Long projectId = task.getProject().getProjectId();

        // 👤 Lấy actor hiện tại (dù local hay Google login)
        User actor = getCurrentActor();
        if (actor == null)
            throw new AccessDeniedException(
                    "Bạn chưa đăng nhập hoặc không xác định được tài khoản!");

        String email = actor.getEmail();

        // 🔐 Chỉ PM hoặc ADMIN mới được bỏ gán
        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);
        authz.ensurePmOfProject(email, projectId);

        // ⚙️ Kiểm tra nếu user chưa được gán
        if (!followerRepo.existsByTask_TaskIdAndUser_UserId(taskId, userId)) {
            log.warn("⚠️ User {} không được gán trong task {}", userId, taskId);
            return false;
        }

        // 🧩 Lấy user bị xóa
        User removedUser = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("❌ User không tồn tại"));

        // 💾 Xóa follower
        followerRepo.deleteByTaskAndUser(taskId, userId);
        log.info("🗑️ Đã bỏ gán user {} ({}) khỏi task {}", userId, removedUser.getName(), taskId);

        // 🪶 Ghi activity
        activityService.log("TASK", taskId, "REMOVE_MEMBER",
                "{\"user\":\"" + removedUser.getName() + "\"}", actor);

        // 🔔 Gửi thông báo (bỏ qua nếu actor = target)
        try {
            if (!removedUser.getUserId().equals(actor.getUserId())) {
                String link = "/projects/" + projectId + "/tasks/" + task.getTaskId();
                String title = "Bị xóa khỏi công việc";
                String message = "đã xóa bạn khỏi công việc \"" + task.getTitle() + "\"";
                notificationService.createNotification(removedUser, "TASK_MEMBER_REMOVED",
                        task.getTaskId(), title, message, link, actor);
            }
        } catch (Exception e) {
            log.error("⚠️ Lỗi khi gửi thông báo xóa member: {}", e.getMessage(), e);
        }

        return true;
    }

    private User getCurrentActor() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated())
                return null;

            String email = null;
            if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidc)
                email = oidc.getEmail();
            else if (auth
                    .getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User ou)
                email = String.valueOf(ou.getAttributes().get("email"));
            else if (auth
                    .getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails ud)
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
