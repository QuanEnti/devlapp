package com.devcollab.service.core;

import com.devcollab.domain.Project;
import com.devcollab.domain.ProjectMember;

import com.devcollab.dto.ProjectDTO;
import com.devcollab.dto.response.ProjectDashboardDTO;
import com.devcollab.dto.response.ProjectPerformanceDTO;

import com.devcollab.dto.response.ProjectSearchResponseDTO;
import com.devcollab.dto.userTaskDto.ProjectFilterDTO;


import java.util.List;

import org.springframework.data.domain.Page;

public interface ProjectService {

    Project createProject(Project project, Long creatorId);

    Project updateProject(Long id, Project patch);

    List<Project> getProjectsByUser(Long userId);

    ProjectMember addMember(Long projectId, Long userId, String role);

    void removeMember(Long projectId, Long userId);

    Project archiveProject(Long projectId);

    void deleteProject(Long projectId);
    
    Project getById(Long projectId);
    
    ProjectDashboardDTO getDashboardForPm(Long projectId, String pmEmail);
    
    Project getByIdWithMembers(Long projectId);

    ProjectPerformanceDTO getPerformanceData(Long projectId, String pmEmail);

    List<ProjectDTO> getTopProjects(int limit);
    
    Page<ProjectDTO> getAllProjects(int page, int size, String keyword);

    Project enableShareLink(Long projectId, String pmEmail);

    Project disableShareLink(Long projectId, String pmEmail);
    ProjectMember joinProjectByLink(String inviteLink, Long userId);

    List<Project> getProjectsByUsername(String username);

    List<ProjectSearchResponseDTO> searchProjectsByKeyword(String keyword);

    List<ProjectFilterDTO> getActiveProjectsForUser(Long userId);

}
