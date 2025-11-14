package com.devcollab.service.impl.core;

import com.devcollab.domain.Project;
import com.devcollab.domain.User;
import com.devcollab.dto.ProjectDTO;
import com.devcollab.dto.UserDTO;
import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProjectServiceImplTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProjectServiceImpl userProjectService;

    private User testUser;
    private Project testProject1;
    private Project testProject2;
    private Long testUserId = 1L;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(testUserId);
        testUser.setEmail("test@example.com");

        testProject1 = new Project();
        testProject1.setProjectId(1L);
        testProject1.setName("Project 1");

        testProject2 = new Project();
        testProject2.setProjectId(2L);
        testProject2.setName("Project 2");
    }

    @Test
    void testGetProjectsByUser_Success() {
        // Given
        List<Long> projectIds = Arrays.asList(1L, 2L);
        when(projectMemberRepository.findProjectIdsByUserId(testUserId)).thenReturn(projectIds);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject1));
        when(projectRepository.findById(2L)).thenReturn(Optional.of(testProject2));

        // When
        List<ProjectDTO> result = userProjectService.getProjectsByUser(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(projectMemberRepository).findProjectIdsByUserId(testUserId);
        verify(projectRepository).findById(1L);
        verify(projectRepository).findById(2L);
    }

    @Test
    void testGetProjectsByUser_EmptyList() {
        // Given
        when(projectMemberRepository.findProjectIdsByUserId(testUserId)).thenReturn(List.of());

        // When
        List<ProjectDTO> result = userProjectService.getProjectsByUser(testUserId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(projectMemberRepository).findProjectIdsByUserId(testUserId);
    }

    @Test
    void testGetProjectsByUser_SomeProjectsNotFound() {
        // Given
        List<Long> projectIds = Arrays.asList(1L, 2L);
        when(projectMemberRepository.findProjectIdsByUserId(testUserId)).thenReturn(projectIds);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject1));
        when(projectRepository.findById(2L)).thenReturn(Optional.empty());

        // When
        List<ProjectDTO> result = userProjectService.getProjectsByUser(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(projectMemberRepository).findProjectIdsByUserId(testUserId);
    }

    @Test
    void testGetWorkedWithUsers_Success() {
        // Given
        List<Long> projectIds = Arrays.asList(1L, 2L);
        User otherUser1 = new User();
        otherUser1.setUserId(2L);
        otherUser1.setEmail("user2@example.com");
        User otherUser2 = new User();
        otherUser2.setUserId(3L);
        otherUser2.setEmail("user3@example.com");
        List<User> users = Arrays.asList(otherUser1, otherUser2);

        when(projectMemberRepository.findProjectIdsByUserId(testUserId)).thenReturn(projectIds);
        when(projectMemberRepository.findUsersByProjectIds(projectIds, testUserId)).thenReturn(users);

        // When
        List<UserDTO> result = userProjectService.getWorkedWithUsers(testUserId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(projectMemberRepository).findProjectIdsByUserId(testUserId);
        verify(projectMemberRepository).findUsersByProjectIds(projectIds, testUserId);
    }

    @Test
    void testGetWorkedWithUsers_NoProjects() {
        // Given
        when(projectMemberRepository.findProjectIdsByUserId(testUserId)).thenReturn(List.of());

        // When
        List<UserDTO> result = userProjectService.getWorkedWithUsers(testUserId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(projectMemberRepository).findProjectIdsByUserId(testUserId);
        verify(projectMemberRepository, never()).findUsersByProjectIds(any(), any());
    }
}

