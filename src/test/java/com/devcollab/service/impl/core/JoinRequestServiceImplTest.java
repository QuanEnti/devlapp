package com.devcollab.service.impl.core;

import com.devcollab.domain.*;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.JoinRequestRepository;
import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JoinRequestServiceImplTest {

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private JoinRequestServiceImpl joinRequestService;

    private Project testProject;
    private User testUser;
    private JoinRequest testRequest;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setProjectId(1L);
        testProject.setName("Test Project");

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        testRequest = new JoinRequest();
        testRequest.setId(1L);
        testRequest.setProject(testProject);
        testRequest.setUser(testUser);
        testRequest.setStatus("PENDING");
        testRequest.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateJoinRequest_Success() {
        // Given
        when(projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(1L, 1L))
                .thenReturn(false);
        when(joinRequestRepository.existsByProject_ProjectIdAndUser_UserIdAndStatus(1L, 1L, "PENDING"))
                .thenReturn(false);
        when(joinRequestRepository.save(any(JoinRequest.class))).thenReturn(testRequest);

        // When
        JoinRequest result = joinRequestService.createJoinRequest(testProject, testUser);

        // Then
        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        verify(projectMemberRepository).existsByProject_ProjectIdAndUser_UserId(1L, 1L);
        verify(joinRequestRepository).existsByProject_ProjectIdAndUser_UserIdAndStatus(1L, 1L, "PENDING");
        verify(joinRequestRepository).save(any(JoinRequest.class));
        verify(notificationService).notifyJoinRequestToPM(testProject, testUser);
        verify(activityService).log(anyString(), eq(1L), eq("JOIN_REQUEST"), anyString());
    }

    @Test
    void testCreateJoinRequest_NullProject() {
        // When & Then
        assertThrows(BadRequestException.class, () -> {
            joinRequestService.createJoinRequest(null, testUser);
        });
    }

    @Test
    void testCreateJoinRequest_NullUser() {
        // When & Then
        assertThrows(BadRequestException.class, () -> {
            joinRequestService.createJoinRequest(testProject, null);
        });
    }

    @Test
    void testCreateJoinRequest_AlreadyMember() {
        // Given
        when(projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(1L, 1L))
                .thenReturn(true);

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            joinRequestService.createJoinRequest(testProject, testUser);
        });
    }

    @Test
    void testCreateJoinRequest_PendingRequestExists() {
        // Given
        when(projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(1L, 1L))
                .thenReturn(false);
        when(joinRequestRepository.existsByProject_ProjectIdAndUser_UserIdAndStatus(1L, 1L, "PENDING"))
                .thenReturn(true);

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            joinRequestService.createJoinRequest(testProject, testUser);
        });
    }

    @Test
    void testApproveRequest_Success() {
        // Given
        String reviewerEmail = "reviewer@example.com";
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(testRequest));
        when(projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(1L, 1L))
                .thenReturn(false);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(new ProjectMember());
        when(joinRequestRepository.save(any(JoinRequest.class))).thenReturn(testRequest);

        // When
        JoinRequest result = joinRequestService.approveRequest(1L, reviewerEmail);

        // Then
        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        verify(joinRequestRepository).findById(1L);
        verify(projectMemberRepository).save(any(ProjectMember.class));
        verify(joinRequestRepository).save(any(JoinRequest.class));
        verify(notificationService).notifyJoinRequestApproved(testProject, testUser, reviewerEmail);
        verify(activityService).log(anyString(), eq(1L), eq("JOIN_REQUEST_APPROVED"), anyString());
    }

    @Test
    void testApproveRequest_NotFound() {
        // Given
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            joinRequestService.approveRequest(1L, "reviewer@example.com");
        });
    }

    @Test
    void testApproveRequest_AlreadyProcessed() {
        // Given
        testRequest.setStatus("APPROVED");
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            joinRequestService.approveRequest(1L, "reviewer@example.com");
        });
    }

    @Test
    void testApproveRequest_UserAlreadyMember() {
        // Given
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(testRequest));
        when(projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(1L, 1L))
                .thenReturn(true);

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            joinRequestService.approveRequest(1L, "reviewer@example.com");
        });
    }

    @Test
    void testRejectRequest_Success() {
        // Given
        String reviewerEmail = "reviewer@example.com";
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(testRequest));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenReturn(testRequest);

        // When
        JoinRequest result = joinRequestService.rejectRequest(1L, reviewerEmail);

        // Then
        assertNotNull(result);
        assertEquals("REJECTED", result.getStatus());
        verify(joinRequestRepository).findById(1L);
        verify(joinRequestRepository).save(any(JoinRequest.class));
        verify(notificationService).notifyJoinRequestRejected(testProject, testUser, reviewerEmail);
        verify(activityService).log(anyString(), eq(1L), eq("JOIN_REQUEST_REJECTED"), anyString());
    }

    @Test
    void testRejectRequest_NotFound() {
        // Given
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            joinRequestService.rejectRequest(1L, "reviewer@example.com");
        });
    }

    @Test
    void testRejectRequest_AlreadyProcessed() {
        // Given
        testRequest.setStatus("APPROVED");
        when(joinRequestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            joinRequestService.rejectRequest(1L, "reviewer@example.com");
        });
    }

    @Test
    void testGetPendingRequests_Success() {
        // Given
        List<JoinRequest> expectedRequests = Arrays.asList(testRequest);
        when(joinRequestRepository.findByProject_ProjectIdAndStatus(1L, "PENDING"))
                .thenReturn(expectedRequests);

        // When
        List<JoinRequest> result = joinRequestService.getPendingRequests(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(joinRequestRepository).findByProject_ProjectIdAndStatus(1L, "PENDING");
    }
}

