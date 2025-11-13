package com.devcollab.service.impl.core;

import com.devcollab.domain.*;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.JoinRequestRepository;
import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.service.core.JoinRequestService;
import com.devcollab.service.system.NotificationService;
import com.devcollab.service.system.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JoinRequestServiceImpl implements JoinRequestService {

    private final JoinRequestRepository joinRequestRepo;
    private final ProjectMemberRepository projectMemberRepo;
    private final NotificationService notificationService;
    private final ActivityService activityService;

    // ============================================================
    // 📨 CREATE JOIN REQUEST
    // ============================================================
    @Override
    @Transactional
    public JoinRequest createJoinRequest(Project project, User user) {
        if (project == null || user == null)
            throw new BadRequestException("Thiếu thông tin dự án hoặc người dùng.");

        // Đã là thành viên
        if (projectMemberRepo.existsByProject_ProjectIdAndUser_UserId(project.getProjectId(),
                user.getUserId())) {
            throw new BadRequestException("Bạn đã là thành viên của dự án này!");
        }

        // Đã gửi yêu cầu trước đó
        if (joinRequestRepo.existsByProject_ProjectIdAndUser_UserIdAndStatus(project.getProjectId(),
                user.getUserId(), "PENDING")) {
            throw new BadRequestException("Bạn đã gửi yêu cầu tham gia và đang chờ phê duyệt!");
        }

        JoinRequest req = new JoinRequest();
        req.setProject(project);
        req.setUser(user);
        req.setStatus("PENDING");
        req.setCreatedAt(LocalDateTime.now());
        joinRequestRepo.save(req);

        // 🔔 Gửi thông báo cho tất cả PM/OWNER/ADMIN của project
        notificationService.notifyJoinRequestToPM(project, user);

        // 🧾 Ghi log
        activityService.log("PROJECT", project.getProjectId(), "JOIN_REQUEST",
                user.getEmail() + " đã gửi yêu cầu tham gia dự án.");

        log.info("📩 JoinRequest CREATED by {} for project {}", user.getEmail(), project.getName());
        return req;
    }

    // ============================================================
    // ✅ APPROVE REQUEST
    // ============================================================
    @Override
    @Transactional
    public JoinRequest approveRequest(Long requestId, String reviewerEmail) {
        JoinRequest req = joinRequestRepo.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu tham gia!"));

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new BadRequestException("Yêu cầu đã được xử lý trước đó!");
        }

        Project project = req.getProject();
        User user = req.getUser();

        // Đã là thành viên
        if (projectMemberRepo.existsByProject_ProjectIdAndUser_UserId(project.getProjectId(),
                user.getUserId())) {
            throw new BadRequestException("Người này đã là thành viên dự án!");
        }

        // 🧩 Thêm vào ProjectMember
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRoleInProject("Member");
        member.setJoinedAt(LocalDateTime.now());
        projectMemberRepo.save(member);

        // 🕓 Cập nhật trạng thái yêu cầu
        req.setStatus("APPROVED");
        req.setReviewedAt(LocalDateTime.now());
        req.setReviewedBy(reviewerEmail);
        joinRequestRepo.save(req);

        // 🔔 Gửi thông báo
        notificationService.notifyJoinRequestApproved(project, user, reviewerEmail);

        // 🧾 Ghi log
        activityService.log("PROJECT", project.getProjectId(), "JOIN_REQUEST_APPROVED",
                reviewerEmail + " đã duyệt yêu cầu tham gia của " + user.getEmail());

        log.info("✅ JoinRequest APPROVED by {} for {}", reviewerEmail, user.getEmail());
        return req;
    }

    // ============================================================
    // ❌ REJECT REQUEST
    // ============================================================
    @Override
    @Transactional
    public JoinRequest rejectRequest(Long requestId, String reviewerEmail) {
        JoinRequest req = joinRequestRepo.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu tham gia!"));

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new BadRequestException("Yêu cầu đã được xử lý trước đó!");
        }

        req.setStatus("REJECTED");
        req.setReviewedAt(LocalDateTime.now());
        req.setReviewedBy(reviewerEmail);
        joinRequestRepo.save(req);

        // 🔔 Thông báo cho người gửi yêu cầu
        notificationService.notifyJoinRequestRejected(req.getProject(), req.getUser(),
                reviewerEmail);

        // 🧾 Ghi log
        activityService.log("PROJECT", req.getProject().getProjectId(), "JOIN_REQUEST_REJECTED",
                reviewerEmail + " đã từ chối yêu cầu tham gia của " + req.getUser().getEmail());

        log.info("❌ JoinRequest REJECTED by {} for {}", reviewerEmail, req.getUser().getEmail());
        return req;
    }

    // ============================================================
    // 📋 GET PENDING REQUESTS
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public List<JoinRequest> getPendingRequests(Long projectId) {
        return joinRequestRepo.findByProject_ProjectIdAndStatus(projectId, "PENDING");
    }
}
