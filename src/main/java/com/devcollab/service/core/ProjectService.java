package com.devcollab.service.core;

import com.devcollab.domain.Project;
import com.devcollab.domain.ProjectMember;

import com.devcollab.domain.User;
import com.devcollab.dto.ProjectDTO;
import com.devcollab.dto.ProjectSummaryDTO;
import com.devcollab.dto.response.ProjectDashboardDTO;
import com.devcollab.dto.response.ProjectPerformanceDTO;

import com.devcollab.dto.response.ProjectSearchResponseDTO;

import java.util.List;
import java.util.Map;

import com.devcollab.dto.userTaskDto.ProjectFilterDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    List<ProjectDTO> getTopProjectsByPm(String email, int limit);

    Page<ProjectDTO> getAllProjectsByPm(String email, int page, int size, String keyword);

    Project enableShareLink(Long projectId, String pmEmail);

    Project disableShareLink(Long projectId, String pmEmail);

    ProjectMember joinProjectByLink(String inviteLink, Long userId);

    List<Project> getProjectsByUsername(String username);

    List<ProjectSearchResponseDTO> searchProjectsByKeyword(String keyword);

    String getUserRoleInProject(Long projectId, Long userId);

    String getUserRoleInProjectByEmail(Long projectId, String email);

    List<ProjectFilterDTO> getActiveProjectsForUser(Long userId);

    boolean existsByNameAndCreatedBy_UserId(String name, Long createdById);

    Map<String, Object> getProgress(Long projectId);

    Map<String, Object> getMetrics(Long projectId);
    Page<ProjectMember> getProjectsByUserSorted(User user, String role, Pageable pageable);
    Page<ProjectSummaryDTO> getProjectsByUserPaginated(String email, int page, int size);
    long countAll();
    long countByStatus(String status);
}
