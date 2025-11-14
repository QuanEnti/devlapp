package com.devcollab.service.impl.feature;

import com.devcollab.config.SpringContext;
import com.devcollab.domain.Label;
import com.devcollab.domain.Project;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.dto.LabelDTO;
import com.devcollab.repository.LabelRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.TaskFollowerRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.service.system.ProjectAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelServiceImplTest {

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskFollowerRepository followerRepository;

    @InjectMocks
    private LabelServiceImpl labelService;

    private Project testProject;
    private Task testTask;
    private User testUser;
    private Label testLabel;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setProjectId(1L);
        testProject.setName("Test Project");

        testTask = new Task();
        testTask.setTaskId(1L);
        testTask.setTitle("Test Task");
        testTask.setProject(testProject);
        testTask.setLabels(new HashSet<>());

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        testLabel = new Label();
        testLabel.setLabelId(1L);
        testLabel.setProject(testProject);
        testLabel.setName("Bug");
        testLabel.setColor("#ff0000");
        testLabel.setCreatedBy(testUser);
    }

    @Test
    void testGetLabelsByProject_Success() {
        // Given
        List<LabelDTO> labels = Arrays.asList(new LabelDTO(1L, "Bug", "#ff0000", 1L, "Test User"));
        when(labelRepository.findByProjectAndKeyword(1L, "bug")).thenReturn(labels);

        // When
        List<LabelDTO> result = labelService.getLabelsByProject(1L, "bug");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(labelRepository).findByProjectAndKeyword(1L, "bug");
    }

    @Test
    void testCreateLabel_AsPM_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("test@example.com", 1L);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(labelRepository.existsByProject_ProjectIdAndNameIgnoreCase(1L, "Bug")).thenReturn(false);
            when(labelRepository.save(any(Label.class))).thenReturn(testLabel);

            // When
            LabelDTO result = labelService.createLabel(1L, "Bug", "#ff0000", testUser);

            // Then
            assertNotNull(result);
            assertEquals("Bug", result.getName());
            verify(projectRepository).findById(1L);
            verify(labelRepository).save(any(Label.class));
        }
    }

    @Test
    void testCreateLabel_AsMember_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doThrow(new AccessDeniedException("Not PM")).when(authz).ensurePmOfProject("test@example.com", 1L);
            when(authz.isMemberOfProject("test@example.com", 1L)).thenReturn(true);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(labelRepository.existsByProject_ProjectIdAndNameIgnoreCase(1L, "Bug")).thenReturn(false);
            when(labelRepository.save(any(Label.class))).thenReturn(testLabel);

            // When
            LabelDTO result = labelService.createLabel(1L, "Bug", "#ff0000", testUser);

            // Then
            assertNotNull(result);
        }
    }

    @Test
    void testCreateLabel_NoPermission() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doThrow(new AccessDeniedException("Not PM")).when(authz).ensurePmOfProject("test@example.com", 1L);
            when(authz.isMemberOfProject("test@example.com", 1L)).thenReturn(false);

            // When & Then
            assertThrows(AccessDeniedException.class, () -> {
                labelService.createLabel(1L, "Bug", "#ff0000", testUser);
            });
        }
    }

    @Test
    void testCreateLabel_DuplicateName() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("test@example.com", 1L);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(labelRepository.existsByProject_ProjectIdAndNameIgnoreCase(1L, "Bug")).thenReturn(true);

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                labelService.createLabel(1L, "Bug", "#ff0000", testUser);
            });
        }
    }

    @Test
    void testUpdateLabel_AsPM_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("test@example.com", 1L);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(testLabel));
            when(labelRepository.save(any(Label.class))).thenReturn(testLabel);

            // When
            LabelDTO result = labelService.updateLabel(1L, "Updated Bug", "#00ff00", testUser);

            // Then
            assertNotNull(result);
            assertEquals("Updated Bug", testLabel.getName());
            assertEquals("#00ff00", testLabel.getColor());
            verify(labelRepository).save(testLabel);
        }
    }

    @Test
    void testUpdateLabel_AsCreator_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doThrow(new AccessDeniedException("Not PM")).when(authz).ensurePmOfProject("test@example.com", 1L);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(testLabel));
            when(labelRepository.save(any(Label.class))).thenReturn(testLabel);

            // When
            LabelDTO result = labelService.updateLabel(1L, "Updated Bug", "#00ff00", testUser);

            // Then
            assertNotNull(result);
        }
    }

    @Test
    void testDeleteLabel_AsPM_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("test@example.com", 1L);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(testLabel));
            doNothing().when(labelRepository).deleteAllTaskRelations(1L);
            doNothing().when(labelRepository).delete(any(Label.class));

            // When
            labelService.deleteLabel(1L, testUser);

            // Then
            verify(labelRepository).deleteAllTaskRelations(1L);
            verify(labelRepository).delete(testLabel);
        }
    }

    @Test
    void testGetLabelById_Success() {
        // Given
        LabelDTO dto = new LabelDTO(1L, "Bug", "#ff0000", 1L, "Test User");
        when(labelRepository.findDtoById(1L)).thenReturn(dto);

        // When
        LabelDTO result = labelService.getLabelById(1L);

        // Then
        assertNotNull(result);
        assertEquals("Bug", result.getName());
        verify(labelRepository).findDtoById(1L);
    }

    @Test
    void testGetLabelById_NotFound() {
        // Given
        when(labelRepository.findDtoById(1L)).thenReturn(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            labelService.getLabelById(1L);
        });
    }

    @Test
    void testGetLabelsByTask_Success() {
        // Given
        testTask.getLabels().add(testLabel);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        // When
        List<LabelDTO> result = labelService.getLabelsByTask(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskRepository).findById(1L);
    }

    @Test
    void testAssignLabelToTask_AsPM_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("test@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(labelRepository.findById(1L)).thenReturn(Optional.of(testLabel));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);

            // When
            labelService.assignLabelToTask(1L, 1L, testUser);

            // Then
            assertTrue(testTask.getLabels().contains(testLabel));
            verify(taskRepository).save(testTask);
        }
    }

    @Test
    void testRemoveLabelFromTask_AsPM_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("test@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            doNothing().when(labelRepository).deleteTaskLabel(1L, 1L);

            // When
            labelService.removeLabelFromTask(1L, 1L, testUser);

            // Then
            verify(labelRepository).deleteTaskLabel(1L, 1L);
        }
    }
}

