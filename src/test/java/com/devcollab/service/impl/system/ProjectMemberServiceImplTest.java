package com.devcollab.service.impl.system;

import com.devcollab.domain.*;
import com.devcollab.dto.MemberDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.*;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.MailService;
import com.devcollab.service.system.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceImplTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityService activityService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ApplicationContext context;

    @Mock
    private PendingInviteRepository pendingInviteRepository;

    @Mock
    private MailService mailService;

    @InjectMocks
    private ProjectMemberServiceImpl projectMemberService;

    private Project testProject;
    private User testUser;
    private User testPm;
    private ProjectMember testMember;

    @BeforeEach
    void setUp() {
        testPm = new User();
        testPm.setUserId(1L);
        testPm.setEmail("pm@example.com");
        testPm.setName("PM User");

        testUser = new User();
        testUser.setUserId(2L);
        testUser.setEmail("user@example.com");
        testUser.setName("Test User");

        testProject = new Project();
        testProject.setProjectId(1L);
        testProject.setName("Test Project");
        testProject.setCreatedBy(testPm);

        testMember = new ProjectMember();
        testMember.setProject(testProject);
        testMember.setUser(testUser);
        testMember.setRoleInProject("Member");
        testMember.setJoinedAt(LocalDateTime.now());
    }

    @Test
    void testGetMembersByProject_Success() {
        // Given
        List<MemberDTO> members = Arrays.asList(new MemberDTO(2L, "Test User", "user@example.com", "Member", null));
        when(projectMemberRepository.findMembersByProject(1L)).thenReturn(members);

        // When
        List<MemberDTO> result = projectMemberService.getMembersByProject(1L, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(projectMemberRepository).findMembersByProject(1L);
    }

    @Test
    void testGetMembersByProject_WithKeyword() {
        // Given
        List<MemberDTO> members = Arrays.asList(new MemberDTO(2L, "Test User", "user@example.com", "Member", null));
        when(projectMemberRepository.searchMembersByProject(1L, "test")).thenReturn(members);

        // When
        List<MemberDTO> result = projectMemberService.getMembersByProject(1L, 10, "test");

        // Then
        assertNotNull(result);
        verify(projectMemberRepository).searchMembersByProject(1L, "test");
    }

    @Test
    void testGetMembersByProject_NullProjectId() {
        // When
        List<MemberDTO> result = projectMemberService.getMembersByProject(null, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(projectMemberRepository, never()).findMembersByProject(any());
    }

    @Test
    void testGetAllMembersByPmEmail_Success() {
        // Given
        List<MemberDTO> members = Arrays.asList(new MemberDTO(2L, "Test User", "user@example.com", "Member", null));
        when(projectMemberRepository.findAllMembersByPmEmail("pm@example.com")).thenReturn(members);

        // When
        List<MemberDTO> result = projectMemberService.getAllMembersByPmEmail("pm@example.com");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(projectMemberRepository).findAllMembersByPmEmail("pm@example.com");
    }

    @Test
    void testGetAllMembersByPmEmail_EmptyEmail() {
        // When
        List<MemberDTO> result = projectMemberService.getAllMembersByPmEmail("");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAllMembers_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<MemberDTO> page = new PageImpl<>(Arrays.asList(new MemberDTO(2L, "Test User", "user@example.com", "Member", null)));
        when(projectMemberRepository.findAllMembers("", pageable)).thenReturn(page);

        // When
        Page<MemberDTO> result = projectMemberService.getAllMembers(0, 10, "");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(projectMemberRepository).findAllMembers("", pageable);
    }

    @Test
    void testRemoveMember_Success() {
        // Given
        List<ProjectMember> members = Arrays.asList(testMember);
        when(projectMemberRepository.findByUser_UserId(2L)).thenReturn(members);
        doNothing().when(projectMemberRepository).deleteAll(any());

        // When
        boolean result = projectMemberService.removeMember(2L);

        // Then
        assertTrue(result);
        verify(projectMemberRepository).deleteAll(members);
    }

    @Test
    void testRemoveMember_NotFound() {
        // Given
        when(projectMemberRepository.findByUser_UserId(2L)).thenReturn(new ArrayList<>());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            projectMemberService.removeMember(2L);
        });
    }

    @Test
    void testRemoveMemberFromProject_Success() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(projectMemberRepository.hasManagerPermission(1L, "pm@example.com", List.of("PM", "ADMIN"))).thenReturn(true);
        when(projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(1L, 2L)).thenReturn(true);
        doNothing().when(projectMemberRepository).deleteByProject_ProjectIdAndUser_UserId(1L, 2L);

        // When
        boolean result = projectMemberService.removeMemberFromProject(1L, 2L, "pm@example.com");

        // Then
        assertTrue(result);
        verify(projectMemberRepository).deleteByProject_ProjectIdAndUser_UserId(1L, 2L);
    }

    @Test
    void testRemoveMemberFromProject_SelfRemove() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            projectMemberService.removeMemberFromProject(1L, 2L, "user@example.com");
        });
    }

    @Test
    void testRemoveMemberFromProject_RemoveOwner() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testPm));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            projectMemberService.removeMemberFromProject(1L, 1L, "pm@example.com");
        });
    }

    @Test
    void testRemoveMemberFromProject_NoPermission() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(projectMemberRepository.hasManagerPermission(1L, "user@example.com", List.of("PM", "ADMIN"))).thenReturn(false);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            projectMemberService.removeMemberFromProject(1L, 2L, "user@example.com");
        });
    }

    @Test
    void testAddMemberToProject_ExistingUser_Success() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findByEmail("pm@example.com")).thenReturn(Optional.of(testPm));
        when(projectMemberRepository.existsByProject_ProjectIdAndUser_EmailAndRoleInProjectIn(1L, "pm@example.com", List.of("PM", "ADMIN"))).thenReturn(true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(1L, 2L)).thenReturn(false);
        doNothing().when(projectMemberRepository).addMember(1L, 2L, "MEMBER");
        when(context.getBean(NotificationService.class)).thenReturn(notificationService);
        doNothing().when(notificationService).notifyMemberAdded(any(), any());
        doNothing().when(mailService).sendNotificationMail(anyString(), anyString(), anyString(), anyString(), anyString());

        // When
        boolean result = projectMemberService.addMemberToProject(1L, "pm@example.com", "user@example.com", "Member");

        // Then
        assertTrue(result);
        verify(projectMemberRepository).addMember(1L, 2L, "MEMBER");
        verify(notificationService).notifyMemberAdded(testProject, testUser);
    }

    @Test
    void testAddMemberToProject_UserAlreadyInProject() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findByEmail("pm@example.com")).thenReturn(Optional.of(testPm));
        when(projectMemberRepository.existsByProject_ProjectIdAndUser_EmailAndRoleInProjectIn(1L, "pm@example.com", List.of("PM", "ADMIN"))).thenReturn(true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(1L, 2L)).thenReturn(true);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            projectMemberService.addMemberToProject(1L, "pm@example.com", "user@example.com", "Member");
        });
    }

    @Test
    void testAddMemberToProject_NoPermission() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findByEmail("pm@example.com")).thenReturn(Optional.of(testPm));
        when(projectMemberRepository.existsByProject_ProjectIdAndUser_EmailAndRoleInProjectIn(1L, "pm@example.com", List.of("PM", "ADMIN"))).thenReturn(false);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            projectMemberService.addMemberToProject(1L, "pm@example.com", "user@example.com", "Member");
        });
    }

    @Test
    void testUpdateMemberRole_Success() {
        // Given
        setupSecurityContext();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("pm@example.com")).thenReturn(Optional.of(testPm));
        when(projectMemberRepository.hasManagerPermission(1L, "pm@example.com", List.of("PM", "ADMIN"))).thenReturn(true);
        doNothing().when(projectMemberRepository).updateMemberRole(1L, 2L, "ADMIN");
        when(context.getBean(NotificationService.class)).thenReturn(notificationService);
        doNothing().when(notificationService).notifyMemberRoleUpdated(any(), any(), any(), anyString());
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString(), any());

        // When
        projectMemberService.updateMemberRole(1L, 2L, "Admin", "pm@example.com");

        // Then
        verify(projectMemberRepository).updateMemberRole(1L, 2L, "ADMIN");
        verify(activityService).log(eq("PROJECT"), eq(1L), eq("UPDATE_MEMBER_ROLE"), anyString(), eq(testPm));
    }

    @Test
    void testUpdateMemberRole_ChangeOwnerRole() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testPm));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            projectMemberService.updateMemberRole(1L, 1L, "Member", "pm@example.com");
        });
    }

    @Test
    void testUpdateMemberRole_NoPermission() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(projectMemberRepository.hasManagerPermission(1L, "user@example.com", List.of("PM", "ADMIN"))).thenReturn(false);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            projectMemberService.updateMemberRole(1L, 2L, "Admin", "user@example.com");
        });
    }

    @Test
    void testUpdateMemberRole_Overload_Success() {
        // Given
        setupSecurityContext();
        List<ProjectMember> members = Arrays.asList(testMember);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("pm@example.com")).thenReturn(Optional.of(testPm));
        when(projectMemberRepository.findByProject_ProjectIdAndUser_UserId(1L, 2L)).thenReturn(members);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(testMember);
        when(context.getBean(NotificationService.class)).thenReturn(notificationService);
        doNothing().when(notificationService).notifyMemberRoleUpdated(any(), any(), any(), anyString());

        // When
        boolean result = projectMemberService.updateMemberRole(1L, 2L, "Admin");

        // Then
        assertTrue(result);
        assertEquals("ADMIN", testMember.getRoleInProject());
        verify(projectMemberRepository).save(testMember);
    }

    @Test
    void testRemoveUserFromAllProjectsOfPm_Success() {
        // Given
        List<Project> projects = Arrays.asList(testProject);
        when(userRepository.findByEmail("pm@example.com")).thenReturn(Optional.of(testPm));
        when(projectMemberRepository.findProjectsCreatedByPm("pm@example.com")).thenReturn(projects);
        when(projectMemberRepository.count()).thenReturn(10L, 8L);
        doNothing().when(projectMemberRepository).deleteAllByUserIdAndPmEmail(2L, "pm@example.com");

        // When
        boolean result = projectMemberService.removeUserFromAllProjectsOfPm("pm@example.com", 2L);

        // Then
        assertTrue(result);
        verify(projectMemberRepository).deleteAllByUserIdAndPmEmail(2L, "pm@example.com");
    }

    @Test
    void testRemoveUserFromAllProjectsOfPm_SelfRemove() {
        // Given
        when(userRepository.findByEmail("pm@example.com")).thenReturn(Optional.of(testPm));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            projectMemberService.removeUserFromAllProjectsOfPm("pm@example.com", 1L);
        });
    }

    @Test
    void testRemoveUserFromAllProjectsOfPm_NoProjects() {
        // Given
        when(userRepository.findByEmail("pm@example.com")).thenReturn(Optional.of(testPm));
        when(projectMemberRepository.findProjectsCreatedByPm("pm@example.com")).thenReturn(new ArrayList<>());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            projectMemberService.removeUserFromAllProjectsOfPm("pm@example.com", 2L);
        });
    }

    private void setupSecurityContext() {
        Authentication auth = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("pm@example.com");
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }
}

