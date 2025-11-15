package com.devcollab.service.impl.core;

import com.devcollab.domain.*;
import com.devcollab.dto.MemberDTO;
import com.devcollab.dto.ProjectDTO;
import com.devcollab.dto.ProjectSummaryDTO;
import com.devcollab.dto.response.ProjectDashboardDTO;
import com.devcollab.dto.response.ProjectPerformanceDTO;
import com.devcollab.dto.response.ProjectSearchResponseDTO;
import com.devcollab.dto.userTaskDto.ProjectFilterDTO;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.*;
import com.devcollab.service.core.JoinRequestService;
import com.devcollab.service.event.AppEventService;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import com.devcollab.service.system.ProjectAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private BoardColumnRepository boardColumnRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectAuthorizationService authz;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AppEventService appEventService;

    @Mock
    private ActivityService activityService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private JoinRequestService joinRequestService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private User testCreator;
    private User testUser;
    private Project testProject;
    private Role pmRole;

    @BeforeEach
    void setUp() {
        testCreator = new User();
        testCreator.setUserId(1L);
        testCreator.setEmail("creator@example.com");
        testCreator.setName("Creator User");

        testUser = new User();
        testUser.setUserId(2L);
        testUser.setEmail("user@example.com");
        testUser.setName("Test User");

        testProject = new Project();
        testProject.setProjectId(1L);
        testProject.setName("Test Project");
        testProject.setDescription("Test Description");
        testProject.setCreatedBy(testCreator);
        testProject.setStatus("Active");
        testProject.setPriority("Normal");
        testProject.setVisibility("private");
        testProject.setCreatedAt(LocalDateTime.now());
        testProject.setUpdatedAt(LocalDateTime.now());

        pmRole = new Role();
        pmRole.setRoleId(1L);
        pmRole.setName("ROLE_PM");
    }

    @Test
    void testCreateProject_Success() {
        // Given
        Project newProject = new Project();
        newProject.setName("New Project");
        newProject.setStartDate(LocalDate.now());
        newProject.setDueDate(LocalDate.now().plusDays(30));
        when(projectRepository.existsByNameIgnoreCaseAndCreatedBy_UserId("New Project", 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testCreator));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(new ProjectMember());
        when(roleRepository.findByName("ROLE_PM")).thenReturn(Optional.of(pmRole));
        when(boardColumnRepository.save(any(BoardColumn.class))).thenReturn(new BoardColumn());
        doNothing().when(appEventService).publishProjectCreated(any(Project.class));

        // When
        Project result = projectService.createProject(newProject, 1L);

        // Then
        assertNotNull(result);
        verify(projectRepository).save(newProject);
        verify(projectMemberRepository).save(any(ProjectMember.class));
        verify(boardColumnRepository, times(5)).save(any(BoardColumn.class));
        verify(appEventService).publishProjectCreated(any(Project.class));
    }

    @Test
    void testCreateProject_NullName() {
        // Given
        Project newProject = new Project();
        newProject.setName(null);

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            projectService.createProject(newProject, 1L);
        });
    }

    @Test
    void testCreateProject_BlankName() {
        // Given
        Project newProject = new Project();
        newProject.setName("   ");

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            projectService.createProject(newProject, 1L);
        });
    }

    @Test
    void testCreateProject_DuplicateName() {
        // Given
        Project newProject = new Project();
        newProject.setName("Existing Project");
        when(projectRepository.existsByNameIgnoreCaseAndCreatedBy_UserId("Existing Project", 1L)).thenReturn(true);

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            projectService.createProject(newProject, 1L);
        });
    }

    @Test
    void testCreateProject_InvalidDates() {
        // Given
        Project newProject = new Project();
        newProject.setName("New Project");
        newProject.setStartDate(LocalDate.now().plusDays(10));
        newProject.setDueDate(LocalDate.now());
        when(projectRepository.existsByNameIgnoreCaseAndCreatedBy_UserId("New Project", 1L)).thenReturn(false);

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            projectService.createProject(newProject, 1L);
        });
    }

    @Test
    void testCreateProject_UserNotFound() {
        // Given
        Project newProject = new Project();
        newProject.setName("New Project");
        when(projectRepository.existsByNameIgnoreCaseAndCreatedBy_UserId("New Project", 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            projectService.createProject(newProject, 1L);
        });
    }

    @Test
    void testUpdateProject_Success() {
        // Given
        Project patch = new Project();
        patch.setName("Updated Project");
        patch.setDescription("Updated Description");
        patch.setPriority("High");
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectRepository.existsByNameIgnoreCaseAndCreatedBy_UserIdAndProjectIdNot("Updated Project", 1L, 1L)).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString());

        // When
        Project result = projectService.updateProject(1L, patch);

        // Then
        assertNotNull(result);
        assertEquals("Updated Project", testProject.getName());
        assertEquals("Updated Description", testProject.getDescription());
        assertEquals("High", testProject.getPriority());
        verify(projectRepository).save(testProject);
        verify(activityService).log(eq("PROJECT"), eq(1L), eq("UPDATE"), anyString());
    }

    @Test
    void testUpdateProject_NotFound() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            projectService.updateProject(1L, new Project());
        });
    }

    @Test
    void testUpdateProject_DuplicateName() {
        // Given
        Project patch = new Project();
        patch.setName("Duplicate Project");
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectRepository.existsByNameIgnoreCaseAndCreatedBy_UserIdAndProjectIdNot("Duplicate Project", 1L, 1L)).thenReturn(true);

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            projectService.updateProject(1L, patch);
        });
    }

    @Test
    void testGetProjectsByUser_Success() {
        // Given
        List<Project> created = Arrays.asList(testProject);
        ProjectMember member = new ProjectMember();
        member.setProject(testProject);
        List<ProjectMember> joined = Arrays.asList(member);
        when(projectRepository.findByCreatedBy_UserId(1L)).thenReturn(created);
        when(projectMemberRepository.findByUser_UserId(1L)).thenReturn(joined);

        // When
        List<Project> result = projectService.getProjectsByUser(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(projectRepository).findByCreatedBy_UserId(1L);
        verify(projectMemberRepository).findByUser_UserId(1L);
    }

    @Test
    void testAddMember_Success() {
        // Given
        User newUser = new User();
        newUser.setUserId(2L);
        newUser.setEmail("newuser@example.com");
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newUser));
        when(projectMemberRepository.findByProject_ProjectId(1L)).thenReturn(new ArrayList<>());
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(new ProjectMember());
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString());
        doNothing().when(appEventService).publishMemberAdded(any(), any());
        doNothing().when(notificationService).notifyMemberAdded(any(), any());

        // When
        ProjectMember result = projectService.addMember(1L, 2L, "Member");

        // Then
        assertNotNull(result);
        verify(projectMemberRepository).save(any(ProjectMember.class));
        verify(activityService).log(eq("PROJECT"), eq(1L), eq("ADD_MEMBER"), anyString());
    }

    @Test
    void testAddMember_AlreadyExists() {
        // Given
        User existingUser = new User();
        existingUser.setUserId(2L);
        ProjectMember existingMember = new ProjectMember();
        existingMember.setUser(existingUser);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(2L)).thenReturn(Optional.of(existingUser));
        when(projectMemberRepository.findByProject_ProjectId(1L)).thenReturn(Arrays.asList(existingMember));

        // When
        ProjectMember result = projectService.addMember(1L, 2L, "Member");

        // Then
        assertNotNull(result);
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void testRemoveMember_Success() {
        // Given
        User memberUser = new User();
        memberUser.setUserId(2L);
        ProjectMember member = new ProjectMember();
        member.setUser(memberUser);
        member.setRoleInProject("Member");
        List<ProjectMember> members = Arrays.asList(
                testProject.getCreatedBy().getUserId().equals(1L) ? 
                    new ProjectMember() {{
                        setUser(testCreator);
                        setRoleInProject("PM");
                    }} : member,
                member
        );
        when(projectMemberRepository.findByProject_ProjectId(1L)).thenReturn(members);
        doNothing().when(projectMemberRepository).delete(any(ProjectMember.class));
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString());

        // When
        projectService.removeMember(1L, 2L);

        // Then
        verify(projectMemberRepository).delete(member);
        verify(activityService).log(eq("PROJECT"), eq(1L), eq("REMOVE_MEMBER"), anyString());
    }

    @Test
    void testRemoveMember_LastPM() {
        // Given
        ProjectMember pmMember = new ProjectMember();
        pmMember.setUser(testCreator);
        pmMember.setRoleInProject("PM");
        List<ProjectMember> members = Arrays.asList(pmMember);
        when(projectMemberRepository.findByProject_ProjectId(1L)).thenReturn(members);

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            projectService.removeMember(1L, 1L);
        });
    }

    @Test
    void testArchiveProject_Success() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString());
        doNothing().when(notificationService).notifyProjectArchived(any(Project.class));

        // When
        Project result = projectService.archiveProject(1L);

        // Then
        assertNotNull(result);
        assertEquals("Archived", testProject.getStatus());
        assertNotNull(testProject.getArchivedAt());
        verify(projectRepository).save(testProject);
    }

    @Test
    void testArchiveProject_AlreadyArchived() {
        // Given
        testProject.setStatus("Archived");
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        // When
        Project result = projectService.archiveProject(1L);

        // Then
        assertNotNull(result);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testDeleteProject_Success() {
        // Given
        when(projectRepository.existsById(1L)).thenReturn(true);
        doNothing().when(projectRepository).deleteById(1L);
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString());

        // When
        projectService.deleteProject(1L);

        // Then
        verify(projectRepository).deleteById(1L);
        verify(activityService).log(eq("PROJECT"), eq(1L), eq("DELETE"), anyString());
    }

    @Test
    void testDeleteProject_NotFound() {
        // Given
        when(projectRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            projectService.deleteProject(1L);
        });
    }

    @Test
    void testGetById_Success() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        // When
        Project result = projectService.getById(1L);

        // Then
        assertNotNull(result);
        assertEquals(testProject, result);
        verify(projectRepository).findById(1L);
    }

    @Test
    void testGetById_NotFound() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            projectService.getById(1L);
        });
    }

    @Test
    void testGetDashboardForPm_Success() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        doNothing().when(authz).ensurePmOfProject("pm@example.com", 1L);
        when(taskRepository.countByProject_ProjectId(1L)).thenReturn(10L);
        when(taskRepository.countByProject_ProjectIdAndStatus(1L, "OPEN")).thenReturn(3L);
        when(taskRepository.countByProject_ProjectIdAndStatus(1L, "IN_PROGRESS")).thenReturn(2L);
        when(taskRepository.countByProject_ProjectIdAndStatus(1L, "REVIEW")).thenReturn(2L);
        when(taskRepository.countByProject_ProjectIdAndStatus(1L, "DONE")).thenReturn(3L);
        when(taskRepository.countOverdue(1L, any(LocalDateTime.class))).thenReturn(1L);

        // When
        ProjectDashboardDTO result = projectService.getDashboardForPm(1L, "pm@example.com");

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getTotalTasks());
        assertEquals(3L, result.getOpenTasks());
        assertEquals(3L, result.getDoneTasks());
        assertEquals(BigDecimal.valueOf(30.00), result.getPercentDone());
        verify(authz).ensurePmOfProject("pm@example.com", 1L);
    }

    @Test
    void testGetPerformanceData_Success() {
        // Given
        doNothing().when(authz).ensurePmOfProject("pm@example.com", 1L);
        List<Object[]> results = Arrays.asList(
                new Object[]{"Mon", 5L},
                new Object[]{"Tue", 8L}
        );
        when(taskRepository.countCompletedTasksPerDay(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(results);
        when(taskRepository.countByProject_ProjectId(1L)).thenReturn(20L);

        // When
        ProjectPerformanceDTO result = projectService.getPerformanceData(1L, "pm@example.com");

        // Then
        assertNotNull(result);
        assertNotNull(result.getLabels());
        assertNotNull(result.getAchieved());
        assertNotNull(result.getTarget());
        verify(authz).ensurePmOfProject("pm@example.com", 1L);
    }

    @Test
    void testEnableShareLink_Success() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        doNothing().when(authz).ensurePmOfProject("pm@example.com", 1L);
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        // When
        Project result = projectService.enableShareLink(1L, "pm@example.com");

        // Then
        assertNotNull(result);
        assertTrue(testProject.isAllowLinkJoin());
        assertNotNull(testProject.getInviteLink());
        verify(projectRepository).save(testProject);
    }

    @Test
    void testDisableShareLink_Success() {
        // Given
        testProject.setAllowLinkJoin(true);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        doNothing().when(authz).ensurePmOfProject("pm@example.com", 1L);
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString());

        // When
        Project result = projectService.disableShareLink(1L, "pm@example.com");

        // Then
        assertNotNull(result);
        assertFalse(testProject.isAllowLinkJoin());
        verify(projectRepository).save(testProject);
    }

    @Test
    void testJoinProjectByLink_Success() {
        // Given
        testProject.setInviteLink("testlink12345678");
        testProject.setAllowLinkJoin(true);
        testProject.setInviteExpiredAt(LocalDateTime.now().plusDays(7));
        testProject.setInviteUsageCount(0);
        testProject.setInviteMaxUses(10);
        testProject.setInviteCreatedBy("pm@example.com");
        when(projectRepository.findByInviteLink("testlink12345678")).thenReturn(Optional.of(testProject));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(1L, 2L)).thenReturn(false);
        when(projectMemberRepository.existsByProject_ProjectIdAndUser_EmailAndRoleInProjectIn(1L, "pm@example.com", List.of("PM", "ADMIN", "OWNER"))).thenReturn(true);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(new ProjectMember());
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString());
        doNothing().when(notificationService).notifyMemberAdded(any(), any());

        // When
        Map<String, Object> result = projectService.joinProjectByLink("testlink12345678", 2L);

        // Then
        assertNotNull(result);
        assertEquals("joined_success", result.get("message"));
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    void testJoinProjectByLink_LinkDisabled() {
        // Given
        testProject.setInviteLink("testlink12345678");
        testProject.setAllowLinkJoin(false);
        when(projectRepository.findByInviteLink("testlink12345678")).thenReturn(Optional.of(testProject));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            projectService.joinProjectByLink("testlink12345678", 2L);
        });
    }

    @Test
    void testJoinProjectByLink_Expired() {
        // Given
        testProject.setInviteLink("testlink12345678");
        testProject.setAllowLinkJoin(true);
        testProject.setInviteExpiredAt(LocalDateTime.now().minusDays(1));
        when(projectRepository.findByInviteLink("testlink12345678")).thenReturn(Optional.of(testProject));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            projectService.joinProjectByLink("testlink12345678", 2L);
        });
    }

    @Test
    void testJoinProjectByLink_AlreadyMember() {
        // Given
        testProject.setInviteLink("testlink12345678");
        testProject.setAllowLinkJoin(true);
        testProject.setInviteExpiredAt(LocalDateTime.now().plusDays(7));
        when(projectRepository.findByInviteLink("testlink12345678")).thenReturn(Optional.of(testProject));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(1L, 2L)).thenReturn(true);

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            projectService.joinProjectByLink("testlink12345678", 2L);
        });
    }

    @Test
    void testSearchProjectsByKeyword_Success() {
        // Given
        List<Project> projects = Arrays.asList(testProject);
        when(projectRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase("test", "test")).thenReturn(projects);

        // When
        List<ProjectSearchResponseDTO> result = projectService.searchProjectsByKeyword("test");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(projectRepository).findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase("test", "test");
    }

    @Test
    void testGetUserRoleInProject_Success() {
        // Given
        when(projectMemberRepository.findRoleInProject(1L, 1L)).thenReturn(Optional.of("PM"));

        // When
        String result = projectService.getUserRoleInProject(1L, 1L);

        // Then
        assertEquals("PM", result);
        verify(projectMemberRepository).findRoleInProject(1L, 1L);
    }

    @Test
    void testGetUserRoleInProject_DefaultMember() {
        // Given
        when(projectMemberRepository.findRoleInProject(1L, 1L)).thenReturn(Optional.empty());

        // When
        String result = projectService.getUserRoleInProject(1L, 1L);

        // Then
        assertEquals("Member", result);
    }

    @Test
    void testCountAll_Success() {
        // Given
        when(projectRepository.count()).thenReturn(10L);

        // When
        long result = projectService.countAll();

        // Then
        assertEquals(10L, result);
        verify(projectRepository).count();
    }

    @Test
    void testCountByStatus_Success() {
        // Given
        when(projectRepository.countByStatus("Active")).thenReturn(5L);

        // When
        long result = projectService.countByStatus("Active");

        // Then
        assertEquals(5L, result);
        verify(projectRepository).countByStatus("Active");
    }
}

