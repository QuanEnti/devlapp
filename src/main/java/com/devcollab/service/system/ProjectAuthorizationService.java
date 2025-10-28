package com.devcollab.service.system;

import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectAuthorizationService {

    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public Long ensurePmOfProject(String email, Long projectId) {
        Long uid = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User không tồn tại"))
                .getUserId();

        boolean isAuthorized = projectMemberRepository
                .existsByProject_ProjectIdAndUser_UserIdAndRoleInProjectIn(projectId, uid, List.of("PM", "ADMIN"));

        if (!isAuthorized)
            throw new AccessDeniedException("Bạn không có quyền PM hoặc ADMIN của dự án này");

        return uid;
    }
}
