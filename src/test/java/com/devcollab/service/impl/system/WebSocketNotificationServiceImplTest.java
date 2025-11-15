package com.devcollab.service.impl.system;

import com.devcollab.domain.Notification;
import com.devcollab.domain.User;
import com.devcollab.dto.response.NotificationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketNotificationServiceImplTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketNotificationServiceImpl webSocketNotificationService;

    private User testReceiver;
    private User testSender;
    private Notification testNotification;

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
        testSender.setAvatarUrl("http://example.com/sender.jpg");

        testNotification = new Notification();
        testNotification.setNotificationId(1L);
        testNotification.setType("TASK_COMMENTED");
        testNotification.setTitle("New Comment");
        testNotification.setMessage("You have a new comment");
        testNotification.setStatus("unread");
        testNotification.setReferenceId(100L);
        testNotification.setLink("/tasks/100");
        testNotification.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testSendToUser_Success() {
        // Given
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(NotificationResponseDTO.class));

        // When
        webSocketNotificationService.sendToUser(testReceiver, testNotification, testSender);

        // Then
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<NotificationResponseDTO> dtoCaptor = ArgumentCaptor.forClass(NotificationResponseDTO.class);
        
        verify(messagingTemplate).convertAndSend(destinationCaptor.capture(), dtoCaptor.capture());
        
        assertEquals("/user/receiver@example.com/queue/notifications", destinationCaptor.getValue());
        NotificationResponseDTO dto = dtoCaptor.getValue();
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("TASK_COMMENTED", dto.getType());
        assertEquals("New Comment", dto.getTitle());
        assertEquals("Sender User", dto.getSenderName());
    }

    @Test
    void testSendToUser_NullReceiver() {
        // When
        webSocketNotificationService.sendToUser(null, testNotification, testSender);

        // Then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void testSendToUser_NullNotification() {
        // When
        webSocketNotificationService.sendToUser(testReceiver, null, testSender);

        // Then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void testSendToUser_NullEmail() {
        // Given
        testReceiver.setEmail(null);

        // When
        webSocketNotificationService.sendToUser(testReceiver, testNotification, testSender);

        // Then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void testSendToUser_BlankEmail() {
        // Given
        testReceiver.setEmail("   ");

        // When
        webSocketNotificationService.sendToUser(testReceiver, testNotification, testSender);

        // Then
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void testSendToUser_NullSender() {
        // Given
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(NotificationResponseDTO.class));

        // When
        webSocketNotificationService.sendToUser(testReceiver, testNotification, null);

        // Then
        ArgumentCaptor<NotificationResponseDTO> dtoCaptor = ArgumentCaptor.forClass(NotificationResponseDTO.class);
        verify(messagingTemplate).convertAndSend(anyString(), dtoCaptor.capture());
        
        NotificationResponseDTO dto = dtoCaptor.getValue();
        assertEquals("Hệ thống", dto.getSenderName());
    }

    @Test
    void testSendToUser_ExceptionHandling() {
        // Given
        doThrow(new RuntimeException("Connection error")).when(messagingTemplate)
                .convertAndSend(anyString(), any(NotificationResponseDTO.class));

        // When - Should not throw exception
        assertDoesNotThrow(() -> {
            webSocketNotificationService.sendToUser(testReceiver, testNotification, testSender);
        });
    }

    @Test
    void testSendToUser_IconMapping() {
        // Given
        testNotification.setType("TASK_DUE_SOON");
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(NotificationResponseDTO.class));

        // When
        webSocketNotificationService.sendToUser(testReceiver, testNotification, testSender);

        // Then
        ArgumentCaptor<NotificationResponseDTO> dtoCaptor = ArgumentCaptor.forClass(NotificationResponseDTO.class);
        verify(messagingTemplate).convertAndSend(anyString(), dtoCaptor.capture());
        
        NotificationResponseDTO dto = dtoCaptor.getValue();
        assertEquals("⏰", dto.getIcon());
    }
}

