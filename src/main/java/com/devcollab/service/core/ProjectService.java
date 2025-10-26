package com.devcollab.service.core;

import com.devcollab.domain.Project;
import com.devcollab.domain.ProjectMember;
import com.devcollab.dto.response.ProjectSearchResponseDTO;

import java.util.List;

public interface ProjectService {

    Project createProject(Project project, Long creatorId);

    Project updateProject(Long id, Project patch);

    List<Project> getProjectsByUser(Long userId);

    ProjectMember addMember(Long projectId, Long userId, String role);

    void removeMember(Long projectId, Long userId);

    Project archiveProject(Long projectId);

    void deleteProject(Long projectId);
    
    Project getById(Long projectId);

    List<Project> getProjectsByUsername(String username);

    List<ProjectSearchResponseDTO> searchProjectsByKeyword(String keyword);

}
