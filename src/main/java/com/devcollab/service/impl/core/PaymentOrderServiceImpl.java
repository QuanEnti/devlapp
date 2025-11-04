package com.devcollab.service.impl.core;

import com.devcollab.domain.PaymentOrder;
import com.devcollab.domain.User;
import com.devcollab.repository.ActivityRepository;
import com.devcollab.repository.PaymentOrderRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.core.PaymentOrderService;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentOrderServiceImpl implements PaymentOrderService {

    private final PaymentOrderRepository orderRepo;
    private final UserRepository userRepo;
    private final ActivityRepository activityRepo;
    private final ActivityService activityService;
    private final NotificationService notificationService;

    /** ✅ 1. Tạo đơn hàng */
    @Override
    public Map<String, Object> createOrder(Map<String, Object> body, String userEmail) {
        Map<String, Object> res = new HashMap<>();
        try {
            BigDecimal total = new BigDecimal(body.get("total").toString());
            String plan = body.getOrDefault("plan", "Pro").toString();

            PaymentOrder order = new PaymentOrder();
            order.setTotal(total);
            order.setName("Upgrade Plan: " + plan);
            order.setPaymentStatus("Unpaid");
            order.setCreatedAt(Instant.now());

            if (userEmail != null) {
                userRepo.findByEmail(userEmail).ifPresent(order::setUser);
            }

            orderRepo.save(order);

            res.put("success", true);
            res.put("orderId", order.getId());
            res.put("redirectUrl", "/payment/checkout?id=" + order.getId());
        } catch (Exception e) {
            res.put("success", false);
            res.put("error", e.getMessage());
        }
        return res;
    }

    /** ✅ 2. Webhook từ SePay */
    @Override
    public Map<String, Object> handleWebhook(Map<String, Object> payload) {
        System.out.println("📩 Webhook nhận: " + payload);
        try {
            String content = (String) payload.get("content");
            if (content == null) return Map.of("success", false, "message", "No content");

            String orderCode = null;
            for (String word : content.split("\\s+")) {
                if (word.startsWith("DH")) {
                    orderCode = word.trim();
                    break;
                }
            }
            if (orderCode == null) return Map.of("success", false, "message", "Missing order code");

            Long orderId = Long.parseLong(orderCode.substring(2));
            Optional<PaymentOrder> optOrder = orderRepo.findById(orderId);
            if (optOrder.isEmpty()) return Map.of("success", false, "message", "Order not found");

            PaymentOrder order = optOrder.get();
            order.setPaymentStatus("Paid");
            orderRepo.save(order);

            User user = null;
            if (order.getUser() != null) {
                user = userRepo.findById(order.getUser().getUserId()).orElse(null);
            }

            if (user != null && !user.isPremium()) {
                user.setPremium(true);
                user.setPremiumExpiry(Instant.now().plusSeconds(30L * 24 * 60 * 60)); // 30 ngày
                userRepo.save(user);
            }

            System.out.println("✅ Đơn hàng #" + orderId + " cập nhật Paid thành công");
            return Map.of("success", true, "message", "Payment updated");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /** ✅ 3. Kiểm tra trạng thái thanh toán */
    @Override
    public Map<String, Object> checkPaymentStatus(Long orderId) {
        Map<String, Object> result = new HashMap<>();
        PaymentOrder order = orderRepo.findById(orderId).orElse(null);

        if (order == null) {
            result.put("payment_status", "order_not_found");
            return result;
        }

        if ("Paid".equalsIgnoreCase(order.getPaymentStatus()) && order.getUser() != null) {
            Long userId = order.getUser().getUserId();

            userRepo.findById(userId).ifPresent(user -> {
                if (!user.isPremium()) {
                    user.setPremium(true);
                    userRepo.save(user);
                    System.out.println("⭐ User " + user.getEmail() + " đã được đồng bộ Premium!");
                }

                // 🔹 Chống duplicate log
                boolean alreadyLogged = activityRepo.existsByActor_UserIdAndEntityTypeAndEntityIdAndAction(
                        user.getUserId(), "PaymentOrder", order.getId(), "payment_success");

                if (!alreadyLogged) {
                    activityService.logWithActor(
                            user.getUserId(),
                            "PaymentOrder",
                            order.getId(),
                            "payment_success",
                            String.format(
                                    "{\"order_id\":%d,\"amount\":%.2f,\"plan\":\"%s\",\"message\":\"Thanh toán thành công, nâng cấp Premium.\"}",
                                    order.getId(),
                                    order.getTotal(),
                                    order.getName() != null ? order.getName() : "Pro"
                            )
                    );

                    notificationService.notifyPaymentSuccess(user, order);
                    System.out.println("✅ Đã tạo Activity log PAYMENT_SUCCESS cho user " + user.getEmail());
                } else {
                    System.out.println("⚙️ Activity log PAYMENT_SUCCESS đã tồn tại, bỏ qua.");
                }
            });
        }

        result.put("payment_status", order.getPaymentStatus());
        return result;
    }
}
