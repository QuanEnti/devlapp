package com.devcollab.service.impl.system;

import com.devcollab.domain.Project;
import com.devcollab.domain.ProjectMember;
import com.devcollab.dto.MemberDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.system.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepo;
    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;

    // 🟢 Lấy danh sách thành viên giới hạn
    @Transactional
    @Override
    public List<MemberDTO> getMembersByProject(Long projectId, int limit) {
        if (projectId == null)
            return List.of();
        List<MemberDTO> members = projectMemberRepo.findMembersByProject(projectId);
        return members.stream().limit(limit).toList();
    }

    @Transactional
    @Override
    public List<MemberDTO> getAllMembersByPmEmail(String email) {
        if (email == null || email.isEmpty())
            return List.of();
        return projectMemberRepo.findAllMembersByPmEmail(email);
    }

    @Transactional
    @Override
    public Page<MemberDTO> getAllMembers(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        return projectMemberRepo.findAllMembers(keyword, pageable);
    }

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

    @Transactional
    public boolean removeMemberFromProject(Long projectId, Long userId) {
        boolean exists = projectMemberRepo
                .existsByProject_ProjectIdAndUser_UserId(projectId, userId);

        if (!exists) {
            throw new RuntimeException("Không tìm thấy thành viên trong dự án!");
        }

        projectMemberRepo.deleteByProject_ProjectIdAndUser_UserId(projectId, userId);
        return true;
    }

    @Transactional
    @Override
    public boolean addMemberToProject(Long projectId, String pmEmail, String email, String role) {
        var project = projectRepo.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án có ID: " + projectId));

        // ✅ CHỈ cho phép người tạo dự án được mời
        if (!project.getCreatedBy().getEmail().equalsIgnoreCase(pmEmail)
                && !project.getCreatedBy().getProviderId().equalsIgnoreCase(pmEmail)) {
            throw new IllegalStateException("Chỉ người tạo dự án mới có quyền mời thành viên!");
        }

        // 🔹 2. Tìm người dùng được mời
        var user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng có email: " + email));

        // 🔹 3. Kiểm tra trùng
        if (projectMemberRepo.existsByProject_ProjectIdAndUser_UserId(projectId, user.getUserId())) {
            throw new IllegalStateException("Người dùng này đã có trong dự án!");
        }

        // 🔹 4. Thêm thành viên mới
        projectMemberRepo.addMember(projectId, user.getUserId(), role.toUpperCase());
        log.info("✅ {} (owner) mời {} vào project ID {} ({}) với vai trò {}",
                pmEmail, email, projectId, project.getName(), role);

        return true;
    }

    @Transactional
    @Override
    public boolean updateMemberRole(Long projectId, Long userId, String role) {
        List<ProjectMember> members = projectMemberRepo
                .findByProject_ProjectIdAndUser_UserId(projectId, userId);

        if (members.isEmpty()) {
            throw new NotFoundException("Không tìm thấy thành viên trong dự án.");
        }

        ProjectMember member = members.get(0);
        member.setRoleInProject(role);
        projectMemberRepo.save(member);
        return true;
    }

    @Transactional
    @Override
    public boolean removeUserFromAllProjectsOfPm(String pmEmail, Long userId) {
        List<Project> projects = projectMemberRepo.findProjectsCreatedByPm(pmEmail);

        if (projects.isEmpty()) {
            throw new NotFoundException("PM chưa có dự án nào để xoá thành viên!");
        }

        long beforeCount = projectMemberRepo.count();
        projectMemberRepo.deleteAllByUserIdAndPmEmail(userId, pmEmail);
        long afterCount = projectMemberRepo.count();

        return beforeCount != afterCount;
    }
    
    @Override
    @Transactional
    public void updateMemberRole(Long projectId, Long userId, String role, String pmEmail) {
        var project = projectRepo.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));

        boolean isOwner = project.getCreatedBy().getEmail().equalsIgnoreCase(pmEmail);
        boolean isManager = projectMemberRepo.hasManagerPermission(projectId, pmEmail, List.of("PM", "ADMIN"));

        if (!isOwner && !isManager) {
            throw new IllegalStateException("Bạn không có quyền đổi vai trò thành viên!");
        }

        projectMemberRepo.updateMemberRole(projectId, userId, role.toUpperCase());
        log.info("🔄 {} đổi vai trò user_id={} thành {}", pmEmail, userId, role);
    }

}
