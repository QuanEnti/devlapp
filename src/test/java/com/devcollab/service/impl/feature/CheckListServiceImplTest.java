package com.devcollab.service.impl.feature;

import com.devcollab.config.SpringContext;
import com.devcollab.domain.CheckList;
import com.devcollab.domain.Project;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.dto.CheckListDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.CheckListRepository;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckListServiceImplTest {

    @Mock
    private CheckListRepository checkListRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskFollowerRepository followerRepository;

    @InjectMocks
    private CheckListServiceImpl checkListService;

    private Task testTask;
    private Project testProject;
    private User testUser;
    private CheckList testCheckList;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setProjectId(1L);
        testProject.setName("Test Project");

        testTask = new Task();
        testTask.setTaskId(1L);
        testTask.setTitle("Test Task");
        testTask.setProject(testProject);

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        testCheckList = new CheckList();
        testCheckList.setChecklistId(1L);
        testCheckList.setTask(testTask);
        testCheckList.setItem("Test Item");
        testCheckList.setIsDone(false);
        testCheckList.setCreatedBy(testUser);
        testCheckList.setOrderIndex(0);
    }

    @Test
    void testGetByTask_Success() {
        // Given
        List<CheckList> checkLists = Arrays.asList(testCheckList);
        when(checkListRepository.findByTask_TaskIdOrderByOrderIndex(1L)).thenReturn(checkLists);

        // When
        List<CheckListDTO> result = checkListService.getByTask(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(checkListRepository).findByTask_TaskIdOrderByOrderIndex(1L);
    }

    @Test
    void testAddItem_AsPM_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("test@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(checkListRepository.findByTask_TaskIdOrderByOrderIndex(1L)).thenReturn(List.of());
            when(checkListRepository.save(any(CheckList.class))).thenReturn(testCheckList);

            // When
            CheckListDTO result = checkListService.addItem(1L, "New Item", testUser);

            // Then
            assertNotNull(result);
            verify(taskRepository).findById(1L);
            verify(checkListRepository).save(any(CheckList.class));
        }
    }

    @Test
    void testAddItem_AsFollower_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doThrow(new AccessDeniedException("Not PM")).when(authz).ensurePmOfProject("test@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(followerRepository.existsByTask_TaskIdAndUser_UserId(1L, 1L)).thenReturn(true);
            when(checkListRepository.findByTask_TaskIdOrderByOrderIndex(1L)).thenReturn(List.of());
            when(checkListRepository.save(any(CheckList.class))).thenReturn(testCheckList);

            // When
            CheckListDTO result = checkListService.addItem(1L, "New Item", testUser);

            // Then
            assertNotNull(result);
            verify(followerRepository).existsByTask_TaskIdAndUser_UserId(1L, 1L);
        }
    }

    @Test
    void testAddItem_TaskNotFound() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            checkListService.addItem(1L, "New Item", testUser);
        });
    }

    @Test
    void testAddItem_NoPermission() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doThrow(new AccessDeniedException("Not PM")).when(authz).ensurePmOfProject("test@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(followerRepository.existsByTask_TaskIdAndUser_UserId(1L, 1L)).thenReturn(false);

            // When & Then
            assertThrows(AccessDeniedException.class, () -> {
                checkListService.addItem(1L, "New Item", testUser);
            });
        }
    }

    @Test
    void testToggleItem_AsPM_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("test@example.com", 1L);
            when(checkListRepository.findById(1L)).thenReturn(Optional.of(testCheckList));
            when(checkListRepository.save(any(CheckList.class))).thenReturn(testCheckList);

            // When
            CheckListDTO result = checkListService.toggleItem(1L, true, testUser);

            // Then
            assertNotNull(result);
            assertTrue(testCheckList.getIsDone());
            verify(checkListRepository).save(testCheckList);
        }
    }

    @Test
    void testToggleItem_NotFound() {
        // Given
        when(checkListRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            checkListService.toggleItem(1L, true, testUser);
        });
    }

    @Test
    void testToggleItem_NoPermission() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doThrow(new AccessDeniedException("Not PM")).when(authz).ensurePmOfProject("test@example.com", 1L);
            when(checkListRepository.findById(1L)).thenReturn(Optional.of(testCheckList));

            // When & Then
            assertThrows(AccessDeniedException.class, () -> {
                checkListService.toggleItem(1L, true, testUser);
            });
        }
    }

    @Test
    void testDeleteItem_AsPM_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("test@example.com", 1L);
            when(checkListRepository.findById(1L)).thenReturn(Optional.of(testCheckList));
            doNothing().when(checkListRepository).delete(any(CheckList.class));

            // When
            checkListService.deleteItem(1L, testUser);

            // Then
            verify(checkListRepository).delete(testCheckList);
        }
    }

    @Test
    void testDeleteItem_AsCreator_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doThrow(new AccessDeniedException("Not PM")).when(authz).ensurePmOfProject("test@example.com", 1L);
            when(checkListRepository.findById(1L)).thenReturn(Optional.of(testCheckList));
            doNothing().when(checkListRepository).delete(any(CheckList.class));

            // When
            checkListService.deleteItem(1L, testUser);

            // Then
            verify(checkListRepository).delete(testCheckList);
        }
    }

    @Test
    void testDeleteItem_NotFound() {
        // Given
        when(checkListRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            checkListService.deleteItem(1L, testUser);
        });
    }

    @Test
    void testDeleteItem_NoPermission() {
        // Given
        User otherUser = new User();
        otherUser.setUserId(2L);
        otherUser.setEmail("other@example.com");
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doThrow(new AccessDeniedException("Not PM")).when(authz).ensurePmOfProject("other@example.com", 1L);
            when(checkListRepository.findById(1L)).thenReturn(Optional.of(testCheckList));

            // When & Then
            assertThrows(AccessDeniedException.class, () -> {
                checkListService.deleteItem(1L, otherUser);
            });
        }
    }
}

