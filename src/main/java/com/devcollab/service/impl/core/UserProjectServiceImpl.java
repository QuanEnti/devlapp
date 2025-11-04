package com.devcollab.service.impl.core;

import com.devcollab.domain.User;
import com.devcollab.dto.ProjectDTO;
import com.devcollab.dto.UserDTO;
import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.core.UserProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProjectServiceImpl implements UserProjectService {

    private final ProjectMemberRepository projectMemberRepo;
    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;

    @Override
    public List<ProjectDTO> getProjectsByUser(Long userId) {
        List<Long> projectIds = projectMemberRepo.findProjectIdsByUserId(userId);

        return projectIds.stream()
                .map(projectRepo::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ProjectDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> getWorkedWithUsers(Long userId) {
        List<Long> projectIds = projectMemberRepo.findProjectIdsByUserId(userId);
        if (projectIds.isEmpty()) return List.of();

        List<User> users = projectMemberRepo.findUsersByProjectIds(projectIds, userId);
        return users.stream()
                .map(UserDTO::new)
                .collect(Collectors.toList());
    }
}
