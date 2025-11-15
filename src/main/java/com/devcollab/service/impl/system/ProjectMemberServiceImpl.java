package com.devcollab.service.impl.system;

import com.devcollab.config.SpringContext;
import com.devcollab.domain.PendingInvite;
import com.devcollab.domain.Project;
import com.devcollab.domain.ProjectMember;
import com.devcollab.domain.User;
import com.devcollab.dto.MemberDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.PendingInviteRepository;
import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.MailService;
import com.devcollab.service.system.NotificationService;
import com.devcollab.service.system.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepo;
    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;
    private final ActivityService activityService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ApplicationContext context;
    @Autowired
    private PendingInviteRepository pendingInviteRepo;
    @Autowired
    private MailService mailService;

    private NotificationService getNotificationService() {
        return context.getBean(NotificationService.class);
    }

    // 🟢 Lấy danh sách thành viên trong project (giới hạn)
    @Transactional(readOnly = true)
    @Override
    public List<MemberDTO> getMembersByProject(Long projectId, int limit) {
        if (projectId == null)
            return List.of();
        return projectMemberRepo.findMembersByProject(projectId).stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<MemberDTO> getMembersByProject(Long projectId, int limit, String keyword) {
        if (projectId == null)
            return List.of();
        keyword = (keyword == null) ? "" : keyword.trim().toLowerCase();

        List<MemberDTO> members =
                keyword.isEmpty() ? projectMemberRepo.findMembersByProject(projectId)
                        : projectMemberRepo.searchMembersByProject(projectId, keyword);

        return members.stream().limit(limit).toList();
    }

    // 🧩 Lấy tất cả member của PM (dùng ở trang tổng quan)
    @Transactional(readOnly = true)
    @Override
    public List<MemberDTO> getAllMembersByPmEmail(String email) {
        if (email == null || email.isEmpty())
            return List.of();
        return projectMemberRepo.findAllMembersByPmEmail(email);
    }

    // 🧭 Phân trang danh sách thành viên
    @Transactional(readOnly = true)
    @Override
    public Page<MemberDTO> getAllMembers(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        return projectMemberRepo.findAllMembers(keyword, pageable);
    }

    // 🧹 Xóa toàn bộ membership của user (Admin)
    @Transactional
    @Override
    public boolean removeMember(Long userId) {
        List<ProjectMember> members = projectMemberRepo.findByUser_UserId(userId);
        if (members == null || members.isEmpty()) {
            throw new NotFoundException("Không tìm thấy thành viên cần xóa");
        }
        projectMemberRepo.deleteAll(members);
        return true;
    }

    // 🔥 Xóa 1 member khỏi project (có phân quyền)
    @Transactional
    @Override
    public boolean removeMemberFromProject(Long projectId, Long userId, String requesterEmail) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án!"));

        User target = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng cần xóa!"));

        // 🚫 Không thể tự xóa chính mình
        if (target.getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new IllegalStateException("Không thể tự xóa chính mình khỏi dự án!");
        }

        // 🚫 Không thể xóa chủ sở hữu dự án
        if (target.getUserId().equals(project.getCreatedBy().getUserId())) {
            throw new IllegalStateException("Không thể xóa người tạo dự án!");
        }

        // ✅ Kiểm tra quyền
        boolean isOwner = project.getCreatedBy().getEmail().equalsIgnoreCase(requesterEmail);
        boolean isManager = projectMemberRepo.hasManagerPermission(projectId, requesterEmail,
                List.of("PM", "ADMIN"));
        if (!isOwner && !isManager) {
            throw new IllegalStateException("Bạn không có quyền xóa thành viên trong dự án này!");
        }

        boolean exists =
                projectMemberRepo.existsByProject_ProjectIdAndUser_UserId(projectId, userId);
        if (!exists)
            throw new NotFoundException("Thành viên không tồn tại trong dự án!");

        // ✅ Thực hiện xóa
        projectMemberRepo.deleteByProject_ProjectIdAndUser_UserId(projectId, userId);
        log.info("🗑️ {} đã xóa {} khỏi project '{}' (ID={})", requesterEmail, target.getEmail(),
                project.getName(), projectId);


        return true;
    }

    @Transactional
    @Override
    public boolean addMemberToProject(Long projectId, String pmEmail, String email, String role) {
        Project project = projectRepo.findById(projectId).orElseThrow(
                () -> new NotFoundException("Không tìm thấy dự án có ID: " + projectId));

        User pm = userRepo.findByEmail(pmEmail)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người mời!"));

        // ✅ Kiểm tra quyền PM/ADMIN (không chỉ creator)
        boolean isPm = projectMemberRepo.existsByProject_ProjectIdAndUser_EmailAndRoleInProjectIn(
                projectId, pmEmail, List.of("PM", "ADMIN"));
        if (!isPm) {
            throw new IllegalStateException("Bạn không có quyền mời thành viên vào dự án này!");
        }
        var userOpt = userRepo.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (projectMemberRepo.existsByProject_ProjectIdAndUser_UserId(projectId,
                    user.getUserId())) {
                throw new IllegalStateException("Người dùng này đã có trong dự án!");
            }

            projectMemberRepo.addMember(projectId, user.getUserId(), role.toUpperCase());
            log.info("✅ {} mời {} vào project '{}' với vai trò {}", pmEmail, email,
                    project.getName(), role);


            notificationService.notifyMemberAdded(project, user);
            mailService.sendNotificationMail(user.getEmail(),
                    "Lời mời tham gia dự án " + project.getName(),
                    pm.getName() + " đã mời bạn tham gia dự án này trên DevCollab.",
                    "/view/pm/project/board?projectId=" + projectId, pm.getName());
            return true;
        }

        if (pendingInviteRepo.existsByEmailAndAcceptedFalse(email)) {
            throw new IllegalStateException("Email này đã được mời nhưng chưa đăng ký.");
        }

        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        PendingInvite invite = new PendingInvite();
        invite.setProjectId(projectId);
        invite.setEmail(email);
        invite.setRole(role);
        invite.setToken(token);
        invite.setAccepted(false);
        invite.setCreatedAt(LocalDateTime.now());
        invite.setExpiresAt(LocalDateTime.now().plusDays(7));
        pendingInviteRepo.save(invite);

        mailService.sendInviteRegistrationMail(email, project, pm, token);
        log.info("📨 Đã gửi email mời đăng ký tới {} cho project '{}'", email, project.getName());

        return true;
    }



    @Transactional
    @Override
    public void updateMemberRole(Long projectId, Long userId, String newRole, String actorEmail) {
        // 🔍 Lấy thông tin dự án và user
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new NotFoundException("❌ Không tìm thấy dự án!"));

        User target = userRepo.findById(userId).orElseThrow(
                () -> new NotFoundException("❌ Không tìm thấy người dùng cần đổi vai trò!"));

        // 🧍‍♂️ Lấy actor hiện tại (ưu tiên SecurityContext)
        User actor = getCurrentActor();
        if (actor == null && actorEmail != null) {
            actor = userRepo.findByEmail(actorEmail).orElseThrow(
                    () -> new NotFoundException("❌ Không tìm thấy người thực hiện hành động!"));
        }
        if (actor == null) {
            throw new IllegalStateException("🚫 Không xác định được người thực hiện hành động!");
        }

        // 🚫 Không cho đổi role của Owner
        if (target.getUserId().equals(project.getCreatedBy().getUserId())) {
            throw new IllegalStateException("🚫 Không thể thay đổi vai trò của người tạo dự án!");
        }

        // 🔐 Kiểm tra quyền: chỉ Owner hoặc PM/ADMIN được đổi vai trò
        boolean isOwner = project.getCreatedBy().getEmail().equalsIgnoreCase(actor.getEmail());
        boolean isManager = projectMemberRepo.hasManagerPermission(projectId, actor.getEmail(),
                List.of("PM", "ADMIN"));
        if (!isOwner && !isManager) {
            throw new IllegalStateException("⚠️ Bạn không có quyền đổi vai trò trong dự án này!");
        }

        // 📝 Cập nhật role
        projectMemberRepo.updateMemberRole(projectId, userId, newRole.toUpperCase());
        log.info("🔄 {} đổi vai trò của {} trong project '{}' thành {}", actor.getEmail(),
                target.getEmail(), project.getName(), newRole);

        // 🪶 Ghi activity (ai đổi, đổi ai, đổi thành gì)
        activityService.log("PROJECT", projectId, "UPDATE_MEMBER_ROLE",
                String.format("{\"actor\":\"%s\",\"target\":\"%s\",\"newRole\":\"%s\"}",
                        actor.getName(), target.getName(), newRole),
                actor);
        try {
            if (!actor.getUserId().equals(target.getUserId())) {

                getNotificationService().notifyMemberRoleUpdated(project, target, actor, newRole);

            } else {
                log.debug("ℹ️ Bỏ qua notify vì actor = target ({})", target.getEmail());
            }
        } catch (Exception e) {
            log.error("⚠️ Lỗi khi gửi thông báo đổi vai trò: {}", e.getMessage(), e);
        }
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

    @Transactional
    @Override
    public boolean removeUserFromAllProjectsOfPm(String pmEmail, Long userId) {
        User pm = userRepo.findByEmail(pmEmail).orElseThrow(
                () -> new NotFoundException("Không tìm thấy người dùng đang đăng nhập!"));

        if (pm.getUserId().equals(userId)) {
            throw new IllegalStateException("Không thể xóa chính bạn khỏi các dự án bạn quản lý!");
        }

        List<Project> projects = projectMemberRepo.findProjectsCreatedByPm(pmEmail);
        if (projects.isEmpty()) {
            throw new NotFoundException("Bạn chưa có dự án nào để xóa thành viên!");
        }

        long before = projectMemberRepo.count();
        projectMemberRepo.deleteAllByUserIdAndPmEmail(userId, pmEmail);
        long after = projectMemberRepo.count();

        return before != after;
    }

    @Transactional
    @Override
    public boolean updateMemberRole(Long projectId, Long userId, String role) {
        // 🔍 Lấy thông tin project & user
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new NotFoundException("❌ Không tìm thấy dự án."));
        User target = userRepo.findById(userId).orElseThrow(
                () -> new NotFoundException("❌ Không tìm thấy người dùng cần đổi vai trò."));

        // 🚫 Không cho đổi role của người tạo dự án
        if (target.getUserId().equals(project.getCreatedBy().getUserId())) {
            throw new IllegalStateException("🚫 Không thể thay đổi vai trò của người tạo dự án!");
        }

        // 🧍‍♂️ Lấy actor hiện tại (ưu tiên SecurityContext)
        User actor = getCurrentActor();
        if (actor == null) {
            log.warn("⚠️ Không xác định được người thực hiện, dùng chủ dự án làm mặc định.");
            actor = project.getCreatedBy();
        }

        // 🧩 Cập nhật role
        List<ProjectMember> members =
                projectMemberRepo.findByProject_ProjectIdAndUser_UserId(projectId, userId);
        if (members.isEmpty())
            throw new NotFoundException("❌ Thành viên không tồn tại trong dự án!");

        ProjectMember m = members.get(0);
        m.setRoleInProject(role.toUpperCase());
        projectMemberRepo.save(m);

        log.info("🔄 {} đổi vai trò của {} trong dự án '{}' thành {}", actor.getEmail(),
                target.getEmail(), project.getName(), role);

        // 🔔 Gửi thông báo realtime
        try {
            getNotificationService().notifyMemberRoleUpdated(project, target, actor, role);
            log.info("📨 [Notification] Sent PROJECT_MEMBER_ROLE_UPDATED to {}", target.getEmail());
        } catch (Exception e) {
            log.error("⚠️ Lỗi khi gửi thông báo đổi vai trò: {}", e.getMessage());
        }

        return true;
    }

    // 🚫 Method cũ (bị vô hiệu hóa)
    @Override
    public boolean removeMemberFromProject(Long projectId, Long userId) {
        throw new UnsupportedOperationException(
                "Hãy dùng removeMemberFromProject(Long projectId, Long userId, String requesterEmail) để phân quyền theo project!");
    }
}
