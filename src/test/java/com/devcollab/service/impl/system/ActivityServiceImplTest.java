package com.devcollab.service.impl.system;

import com.devcollab.domain.Activity;
import com.devcollab.domain.User;
import com.devcollab.dto.ActivityDTO;
import com.devcollab.repository.ActivityRepository;
import com.devcollab.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceImplTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ActivityServiceImpl activityService;

    private User testUser;
    private Activity testActivity;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        testActivity = new Activity();
        testActivity.setActivityId(1L);
        testActivity.setEntityType("TASK");
        testActivity.setEntityId(100L);
        testActivity.setAction("CREATE");
        testActivity.setDataJson("{\"title\":\"Test Task\"}");
        testActivity.setActor(testUser);
        testActivity.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testLog_WithActor_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

        // When
        activityService.log("TASK", 100L, "CREATE", "Test message", testUser);

        // Then
        verify(userRepository).findById(1L);
        verify(activityRepository).save(any(Activity.class));
    }

    @Test
    void testLog_WithoutActor_Success() {
        // Given
        when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

        // When
        activityService.log("TASK", 100L, "CREATE", "Test message");

        // Then
        verify(activityRepository).save(any(Activity.class));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void testLog_NullEntityType_Skip() {
        // When
        activityService.log(null, 100L, "CREATE", "Test message", testUser);

        // Then
        verify(activityRepository, never()).save(any());
    }

    @Test
    void testLog_NullEntityId_Skip() {
        // When
        activityService.log("TASK", null, "CREATE", "Test message", testUser);

        // Then
        verify(activityRepository, never()).save(any());
    }

    @Test
    void testLog_NullAction_Skip() {
        // When
        activityService.log("TASK", 100L, null, "Test message", testUser);

        // Then
        verify(activityRepository, never()).save(any());
    }

    @Test
    void testGetActivities_Success() {
        // Given
        List<Activity> activities = Arrays.asList(testActivity);
        when(activityRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("TASK", 100L))
                .thenReturn(activities);

        // When
        List<ActivityDTO> result = activityService.getActivities("TASK", 100L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TASK", result.get(0).getEntityType());
        verify(activityRepository).findByEntityTypeAndEntityIdOrderByCreatedAtDesc("TASK", 100L);
    }

    @Test
    void testGetAllActivities_Success() {
        // Given
        List<Activity> activities = Arrays.asList(testActivity);
        when(activityRepository.findAllByOrderByCreatedAtDesc()).thenReturn(activities);

        // When
        List<Activity> result = activityService.getAllActivities();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(activityRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void testGetAllActivities_Exception() {
        // Given
        when(activityRepository.findAllByOrderByCreatedAtDesc()).thenThrow(new RuntimeException("DB Error"));

        // When
        List<Activity> result = activityService.getAllActivities();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPaginatedActivities_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Activity> page = new PageImpl<>(Arrays.asList(testActivity));
        when(activityRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);

        // When
        Page<Activity> result = activityService.getPaginatedActivities(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(activityRepository).findAllByOrderByCreatedAtDesc(pageable);
    }

    @Test
    void testLogWithActor_Success() {
        // Given
        when(activityRepository.save(any(Activity.class))).thenReturn(testActivity);

        // When
        activityService.logWithActor(1L, "TASK", 100L, "CREATE", "Test message");

        // Then
        verify(activityRepository).save(any(Activity.class));
    }

    @Test
    void testLogWithActor_NullActorId_Skip() {
        // When
        activityService.logWithActor(null, "TASK", 100L, "CREATE", "Test message");

        // Then
        verify(activityRepository, never()).save(any());
    }

    @Test
    void testSearchActivities_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Activity> page = new PageImpl<>(Arrays.asList(testActivity));
        when(activityRepository.searchActivities("test", "create", "task", pageable))
                .thenReturn(page);

        // When
        Page<Activity> result = activityService.searchActivities("test", "create", "task", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(activityRepository).searchActivities("test", "create", "task", pageable);
    }

    @Test
    void testSearchActivities_WithNullFilters() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Activity> page = new PageImpl<>(Arrays.asList(testActivity));
        when(activityRepository.searchActivities(null, null, null, pageable)).thenReturn(page);

        // When
        Page<Activity> result = activityService.searchActivities(null, null, null, pageable);

        // Then
        assertNotNull(result);
        verify(activityRepository).searchActivities(null, null, null, pageable);
    }
}

