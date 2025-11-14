package com.devcollab.service.impl.core;

import com.devcollab.domain.PaymentOrder;
import com.devcollab.domain.User;
import com.devcollab.repository.ActivityRepository;
import com.devcollab.repository.PaymentOrderRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOrderServiceImplTest {

    @Mock
    private PaymentOrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityService activityService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentOrderServiceImpl paymentOrderService;

    private User testUser;
    private PaymentOrder testOrder;
    private Map<String, Object> testBody;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPremium(false);

        testOrder = new PaymentOrder();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setTotal(new BigDecimal("100.00"));
        testOrder.setName("Upgrade Plan: Pro");
        testOrder.setPaymentStatus("Unpaid");
        testOrder.setCreatedAt(Instant.now());

        testBody = new HashMap<>();
        testBody.put("total", "100.00");
        testBody.put("plan", "Pro");
    }

    @Test
    void testCreateOrder_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(orderRepository.save(any(PaymentOrder.class))).thenReturn(testOrder);

        // When
        Map<String, Object> result = paymentOrderService.createOrder(testBody, "test@example.com");

        // Then
        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("orderId"));
        assertNotNull(result.get("redirectUrl"));
        verify(orderRepository).save(any(PaymentOrder.class));
    }

    @Test
    void testCreateOrder_UserNotFound() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(orderRepository.save(any(PaymentOrder.class))).thenReturn(testOrder);

        // When
        Map<String, Object> result = paymentOrderService.createOrder(testBody, "test@example.com");

        // Then
        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        verify(orderRepository).save(any(PaymentOrder.class));
    }

    @Test
    void testCreateOrder_Exception() {
        // Given
        testBody.put("total", "invalid");

        // When
        Map<String, Object> result = paymentOrderService.createOrder(testBody, "test@example.com");

        // Then
        assertNotNull(result);
        assertFalse((Boolean) result.get("success"));
        assertNotNull(result.get("error"));
    }

    @Test
    void testHandleWebhook_Success() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", "Thanh toan don hang DH1");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(PaymentOrder.class))).thenReturn(testOrder);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        Map<String, Object> result = paymentOrderService.handleWebhook(payload);

        // Then
        assertNotNull(result);
        assertEquals(true, result.get("success"));
        assertEquals("Paid", testOrder.getPaymentStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    void testHandleWebhook_NoContent() {
        // Given
        Map<String, Object> payload = new HashMap<>();

        // When
        Map<String, Object> result = paymentOrderService.handleWebhook(payload);

        // Then
        assertNotNull(result);
        assertEquals(false, result.get("success"));
        verify(orderRepository, never()).findById(any());
    }

    @Test
    void testHandleWebhook_MissingOrderCode() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", "No order code here");

        // When
        Map<String, Object> result = paymentOrderService.handleWebhook(payload);

        // Then
        assertNotNull(result);
        assertEquals(false, result.get("success"));
    }

    @Test
    void testHandleWebhook_OrderNotFound() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", "Thanh toan don hang DH999");
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Map<String, Object> result = paymentOrderService.handleWebhook(payload);

        // Then
        assertNotNull(result);
        assertEquals(false, result.get("success"));
    }

    @Test
    void testHandleWebhook_ExtendPremium() {
        // Given
        testUser.setPremium(true);
        testUser.setPremiumExpiry(Instant.now().plusSeconds(10 * 24 * 60 * 60));
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", "Thanh toan don hang DH1");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(PaymentOrder.class))).thenReturn(testOrder);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        paymentOrderService.handleWebhook(payload);

        // Then
        verify(userRepository).save(testUser);
        assertTrue(testUser.getPremiumExpiry().isAfter(Instant.now()));
    }

    @Test
    void testCheckPaymentStatus_Unpaid() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        Map<String, Object> result = paymentOrderService.checkPaymentStatus(1L);

        // Then
        assertNotNull(result);
        assertEquals("Unpaid", result.get("payment_status"));
        verify(notificationService, never()).notifyPaymentSuccess(any(), any());
    }

    @Test
    void testCheckPaymentStatus_Paid_FirstTime() {
        // Given
        testOrder.setPaymentStatus("Paid");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(activityRepository.existsByActor_UserIdAndEntityTypeAndEntityIdAndAction(
                eq(1L), eq("PaymentOrder"), eq(1L), eq("payment_success"))).thenReturn(false);

        // When
        Map<String, Object> result = paymentOrderService.checkPaymentStatus(1L);

        // Then
        assertNotNull(result);
        assertEquals("Paid", result.get("payment_status"));
        assertTrue(testUser.isPremium());
        verify(userRepository).save(testUser);
        verify(activityService).logWithActor(eq(1L), eq("PaymentOrder"), eq(1L), eq("payment_success"), anyString());
        verify(notificationService).notifyPaymentSuccess(testUser, testOrder);
    }

    @Test
    void testCheckPaymentStatus_Paid_AlreadyLogged() {
        // Given
        testOrder.setPaymentStatus("Paid");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(activityRepository.existsByActor_UserIdAndEntityTypeAndEntityIdAndAction(
                eq(1L), eq("PaymentOrder"), eq(1L), eq("payment_success"))).thenReturn(true);

        // When
        Map<String, Object> result = paymentOrderService.checkPaymentStatus(1L);

        // Then
        assertNotNull(result);
        verify(activityService, never()).logWithActor(anyLong(), anyString(), anyLong(), anyString(), anyString());
        verify(notificationService, never()).notifyPaymentSuccess(any(), any());
    }

    @Test
    void testCheckPaymentStatus_OrderNotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Map<String, Object> result = paymentOrderService.checkPaymentStatus(1L);

        // Then
        assertNotNull(result);
        assertEquals("order_not_found", result.get("payment_status"));
    }

    @Test
    void testGetRevenueByMonth_Success() {
        // Given
        List<Object[]> rawData = new ArrayList<>();
        rawData.add(new Object[]{"2024-11", new BigDecimal("1000.00")});
        rawData.add(new Object[]{"2024-12", new BigDecimal("2000.00")});
        when(orderRepository.sumRevenueByMonth()).thenReturn(rawData);

        // When
        List<Map<String, Object>> result = paymentOrderService.getRevenueByMonth();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("2024-11", result.get(0).get("month"));
        assertEquals(new BigDecimal("1000.00"), result.get(0).get("total"));
        verify(orderRepository).sumRevenueByMonth();
    }
}

