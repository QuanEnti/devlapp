package com.devcollab.service.impl.system;

import com.devcollab.domain.*;
import com.devcollab.dto.CommentDTO;
import com.devcollab.repository.NotificationRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.MailService;
import com.devcollab.service.system.UserSettingsService;
import com.devcollab.service.system.WebSocketNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @Mock
    private ActivityService activityService;

    @Mock
    private MailService mailService;

    @Mock
    private UserSettingsService userSettingsService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testReceiver;
    private User testSender;
    private Notification testNotification;
    private Project testProject;
    private Task testTask;
    private UserSettings testSettings;

    @BeforeEach
    void setUp() {
        testReceiver = new User();
        testReceiver.setUserId(1L);
        testReceiver.setEmail("receiver@example.com");
        testReceiver.setName("Receiver User");

        testSender = new User();
        testSender.setUserId(2L);
        testSender.setEmail("sender@example.com");
        testSender.setName("Sender User");

        testProject = new Project();
        testProject.setProjectId(1L);
        testProject.setName("Test Project");
        testProject.setCreatedBy(testSender);
        testProject.setMembers(new ArrayList<>());

        testTask = new Task();
        testTask.setTaskId(1L);
        testTask.setTitle("Test Task");
        testTask.setProject(testProject);
        testTask.setFollowers(new ArrayList<>());

        testNotification = new Notification();
        testNotification.setNotificationId(1L);
        testNotification.setUser(testReceiver);
        testNotification.setSender(testSender);
        testNotification.setType("TASK_COMMENTED");
        testNotification.setTitle("New Comment");
        testNotification.setMessage("You have a new comment");
        testNotification.setStatus("unread");
        testNotification.setCreatedAt(LocalDateTime.now());

        testSettings = new UserSettings();
        testSettings.setEmailEnabled(true);
        testSettings.setEmailHighImmediate(true);
        testSettings.setEmailDigestEnabled(true);
    }

    @Test
    void testCreateNotification_Success() {
        // Given
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenReturn(testNotification);
        when(userSettingsService.getOrDefault(testReceiver)).thenReturn(testSettings);
        doNothing().when(webSocketNotificationService).sendToUser(any(), any(), any());
        doNothing().when(mailService).sendNotificationMail(anyString(), anyString(), anyString(), anyString(), anyString());

        // When
        notificationService.createNotification(testReceiver, "TASK_COMMENTED", 1L, "New Comment", "Message", "/link", testSender);

        // Then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).saveAndFlush(captor.capture());
        Notification saved = captor.getValue();
        assertEquals("TASK_COMMENTED", saved.getType());
        assertEquals("unread", saved.getStatus());
        verify(webSocketNotificationService).sendToUser(eq(testReceiver), any(), eq(testSender));
    }

    @Test
    void testCreateNotification_NullReceiver() {
        // When
        notificationService.createNotification(null, "TASK_COMMENTED", 1L, "Title", "Message", "/link", testSender);

        // Then
        verify(notificationRepository, never()).saveAndFlush(any());
    }

    @Test
    void testCreateNotification_EmailDisabled() {
        // Given
        testSettings.setEmailEnabled(false);
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenReturn(testNotification);
        when(userSettingsService.getOrDefault(testReceiver)).thenReturn(testSettings);
        doNothing().when(webSocketNotificationService).sendToUser(any(), any(), any());

        // When
        notificationService.createNotification(testReceiver, "TASK_COMMENTED", 1L, "Title", "Message", "/link", testSender);

        // Then
        verify(mailService, never()).sendNotificationMail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testNotifyProjectCreated_Success() {
        // Given
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenReturn(testNotification);
        when(userSettingsService.getOrDefault(any())).thenReturn(testSettings);
        doNothing().when(webSocketNotificationService).sendToUser(any(), any(), any());

        // When
        notificationService.notifyProjectCreated(testProject);

        // Then
        verify(notificationRepository).saveAndFlush(any(Notification.class));
    }

    @Test
    void testNotifyProjectCreated_NullProject() {
        // When
        notificationService.notifyProjectCreated(null);

        // Then
        verify(notificationRepository, never()).saveAndFlush(any());
    }

    @Test
    void testNotifyMemberAdded_Success() {
        // Given
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenReturn(testNotification);
        when(userSettingsService.getOrDefault(any())).thenReturn(testSettings);
        doNothing().when(webSocketNotificationService).sendToUser(any(), any(), any());

        // When
        notificationService.notifyMemberAdded(testProject, testReceiver);

        // Then
        verify(notificationRepository).saveAndFlush(any(Notification.class));
    }

    @Test
    void testNotifyJoinRequestToPM_Success() {
        // Given
        ProjectMember pm = new ProjectMember();
        pm.setUser(testSender);
        pm.setRoleInProject("PM");
        testProject.setMembers(Arrays.asList(pm));
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenReturn(testNotification);
        when(userSettingsService.getOrDefault(any())).thenReturn(testSettings);
        doNothing().when(webSocketNotificationService).sendToUser(any(), any(), any());

        // When
        notificationService.notifyJoinRequestToPM(testProject, testReceiver);

        // Then
        verify(notificationRepository, atLeastOnce()).saveAndFlush(any(Notification.class));
    }

    @Test
    void testNotifyJoinRequestApproved_Success() {
        // Given
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenReturn(testNotification);
        when(userSettingsService.getOrDefault(any())).thenReturn(testSettings);
        doNothing().when(webSocketNotificationService).sendToUser(any(), any(), any());

        // When
        notificationService.notifyJoinRequestApproved(testProject, testReceiver, "reviewer@example.com");

        // Then
        verify(notificationRepository).saveAndFlush(any(Notification.class));
    }

    @Test
    void testNotifyJoinRequestRejected_Success() {
        // Given
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenReturn(testNotification);
        when(userSettingsService.getOrDefault(any())).thenReturn(testSettings);
        doNothing().when(webSocketNotificationService).sendToUser(any(), any(), any());

        // When
        notificationService.notifyJoinRequestRejected(testProject, testReceiver, "reviewer@example.com");

        // Then
        verify(notificationRepository).saveAndFlush(any(Notification.class));
    }

    @Test
    void testNotifyTaskEvent_Success() {
        // Given
        TaskFollower follower = new TaskFollower();
        follower.setUser(testReceiver);
        testTask.setFollowers(Arrays.asList(follower));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenReturn(testNotification);
        when(userSettingsService.getOrDefault(any())).thenReturn(testSettings);
        doNothing().when(webSocketNotificationService).sendToUser(any(), any(), any());

        // When
        notificationService.notifyTaskEvent(testTask, testSender, "TASK_COMMENTED", "Comment message");

        // Then
        verify(notificationRepository, atLeastOnce()).saveAndFlush(any(Notification.class));
    }

    @Test
    void testNotifyTaskEvent_InvalidEventType() {
        // When
        notificationService.notifyTaskEvent(testTask, testSender, "INVALID_EVENT", "Message");

        // Then
        verify(notificationRepository, never()).saveAndFlush(any());
    }

    @Test
    void testNotifyMentions_Success() {
        // Given
        CommentDTO mention = new CommentDTO();
        mention.setUserEmail("receiver@example.com");
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(testReceiver));
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenReturn(testNotification);
        when(userSettingsService.getOrDefault(any())).thenReturn(testSettings);
        doNothing().when(webSocketNotificationService).sendToUser(any(), any(), any());
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString());

        // When
        notificationService.notifyMentions(testTask, testSender, Arrays.asList(mention));

        // Then
        verify(notificationRepository).saveAndFlush(any(Notification.class));
    }

    @Test
    void testNotifyMentions_AtCard() {
        // Given
        testTask.setAssignee(testReceiver);
        testTask.setCreatedBy(testReceiver);
        CommentDTO mention = new CommentDTO();
        mention.setUserEmail("@card");
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenReturn(testNotification);
        when(userSettingsService.getOrDefault(any())).thenReturn(testSettings);
        doNothing().when(webSocketNotificationService).sendToUser(any(), any(), any());

        // When
        notificationService.notifyMentions(testTask, testSender, Arrays.asList(mention));

        // Then
        verify(notificationRepository, atLeastOnce()).saveAndFlush(any(Notification.class));
    }

    @Test
    void testNotifyMentions_AtBoard() {
        // Given
        ProjectMember member = new ProjectMember();
        member.setUser(testReceiver);
        testProject.setMembers(Arrays.asList(member));
        CommentDTO mention = new CommentDTO();
        mention.setUserEmail("@board");
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenReturn(testNotification);
        when(userSettingsService.getOrDefault(any())).thenReturn(testSettings);
        doNothing().when(webSocketNotificationService).sendToUser(any(), any(), any());

        // When
        notificationService.notifyMentions(testTask, testSender, Arrays.asList(mention));

        // Then
        verify(notificationRepository, atLeastOnce()).saveAndFlush(any(Notification.class));
    }

    @Test
    void testCountUnread_Success() {
        // Given
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(testReceiver));
        when(notificationRepository.countUnreadByUserId(1L)).thenReturn(5);

        // When
        int result = notificationService.countUnread("receiver@example.com");

        // Then
        assertEquals(5, result);
        verify(notificationRepository).countUnreadByUserId(1L);
    }

    @Test
    void testCountUnread_UserNotFound() {
        // Given
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.empty());

        // When
        int result = notificationService.countUnread("receiver@example.com");

        // Then
        assertEquals(0, result);
    }

    @Test
    void testGetNotificationsByUser_Success() {
        // Given
        List<Notification> notifications = Arrays.asList(testNotification);
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(testReceiver));
        when(notificationRepository.findNotificationsByUserId(1L)).thenReturn(notifications);

        // When
        List<Notification> result = notificationService.getNotificationsByUser("receiver@example.com");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository).findNotificationsByUserId(1L);
    }

    @Test
    void testMarkAsRead_Success() {
        // Given
        testNotification.setUser(testReceiver);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        boolean result = notificationService.markAsRead(1L, "receiver@example.com");

        // Then
        assertTrue(result);
        assertEquals("read", testNotification.getStatus());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void testMarkAsRead_WrongUser() {
        // Given
        testNotification.setUser(testReceiver);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        // When
        boolean result = notificationService.markAsRead(1L, "other@example.com");

        // Then
        assertFalse(result);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testMarkAllAsRead_Success() {
        // Given
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(testReceiver));
        when(notificationRepository.markAllAsReadByUserId(1L)).thenReturn(5);

        // When
        int result = notificationService.markAllAsRead("receiver@example.com");

        // Then
        assertEquals(5, result);
        verify(notificationRepository).markAllAsReadByUserId(1L);
    }

    @Test
    void testDeleteNotification_Success() {
        // Given
        doNothing().when(notificationRepository).deleteById(1L);

        // When
        notificationService.deleteNotification(1L);

        // Then
        verify(notificationRepository).deleteById(1L);
    }

    @Test
    void testFindRecentByProject_Success() {
        // Given
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("id", 1L);
        when(notificationRepository.findRecentByProject(1L)).thenReturn(Arrays.asList(notificationData));

        // When
        List<Map<String, Object>> result = notificationService.findRecentByProject(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository).findRecentByProject(1L);
    }

    @Test
    void testFindRecentByProject_Exception() {
        // Given
        when(notificationRepository.findRecentByProject(1L)).thenThrow(new RuntimeException("DB Error"));

        // When
        List<Map<String, Object>> result = notificationService.findRecentByProject(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

