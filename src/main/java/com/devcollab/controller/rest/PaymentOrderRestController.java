//package com.devcollab.controller.rest;
//
//import com.devcollab.domain.PaymentOrder;
//import com.devcollab.domain.User;
//import com.devcollab.repository.ActivityRepository;
//import com.devcollab.repository.PaymentOrderRepository;
//import com.devcollab.repository.UserRepository;
//import com.devcollab.service.system.ActivityService;
//import com.devcollab.service.system.NotificationService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//import java.math.BigDecimal;
//import java.time.Instant;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//@RestController
//@RequestMapping("/api/payment")
//@RequiredArgsConstructor
//public class PaymentOrderRestController {
//
//    private final PaymentOrderRepository orderRepo;
//    private final UserRepository userRepo;
//    private final NotificationService notificationService;
//    private final ActivityService activityService;
//    private final ActivityRepository activityRepo;
//
//    /** ✅ 1. Tạo đơn hàng (mua gói Pro) */
//    @PostMapping("/create")
//    public Map<String, Object> createOrder(
//            @RequestBody Map<String, Object> body,
//            Authentication auth) {
//
//        Map<String, Object> res = new HashMap<>();
//        try {
//            BigDecimal total = new BigDecimal(body.get("total").toString());
//            String plan = body.getOrDefault("plan", "Pro").toString();
//
//            PaymentOrder order = new PaymentOrder();
//            order.setTotal(total);
//            order.setName("Upgrade Plan: " + plan);
//            order.setPaymentStatus("Unpaid");
//            order.setCreatedAt(Instant.now());
//
//            // 🔹 Gắn user hiện tại
//            if (auth != null && auth.isAuthenticated()) {
//                String email = auth.getName();
//                userRepo.findByEmail(email).ifPresent(order::setUser);
//            }
//
//            orderRepo.save(order);
//
//            res.put("success", true);
//            res.put("orderId", order.getId());
//            res.put("redirectUrl", "/payment/checkout?id=" + order.getId());
//        } catch (Exception e) {
//            e.printStackTrace();
//            res.put("success", false);
//            res.put("error", e.getMessage());
//        }
//        return res;
//    }
//
//    /** ✅ 2. Nhận Webhook từ SePay */
//    @PostMapping("/webhook")
//    public Map<String, Object> handleWebhook(@RequestBody Map<String, Object> payload) {
//        System.out.println("📩 Webhook nhận: " + payload);
//
//        try {
//            // 🔹 Tìm mã đơn hàng trong content (VD: "QAFBAZ4975 SEPAY7261 1 DH16")
//            String content = (String) payload.get("content");
//            if (content == null) return Map.of("success", false, "message", "No content");
//
//            String orderCode = null;
//            for (String word : content.split("\\s+")) {
//                if (word.startsWith("DH")) {
//                    orderCode = word.trim();
//                    break;
//                }
//            }
//            if (orderCode == null) return Map.of("success", false, "message", "Missing order code");
//
//            Long orderId = Long.parseLong(orderCode.substring(2));
//
//            Optional<PaymentOrder> optOrder = orderRepo.findById(orderId);
//            if (optOrder.isEmpty()) return Map.of("success", false, "message", "Order not found");
//
//            PaymentOrder order = optOrder.get();
//            order.setPaymentStatus("Paid");
//            orderRepo.save(order);
//
//            // ✅ Nếu đơn hàng thuộc về user → cập nhật Premium
//            User user = null;
//            if (order.getUser() != null) {
//                Long userId = order.getUser().getUserId();
//                user = userRepo.findById(userId).orElse(null);
//            }
//            if (user != null && !user.isPremium()) {
//                user.setPremium(true);
//                user.setPremiumExpiry(Instant.now().plusSeconds(30L * 24 * 60 * 60)); // 30 ngày
//                userRepo.save(user);
//            }
//
//            System.out.println("✅ Đơn hàng #" + orderId + " cập nhật Paid thành công");
//            return Map.of("success", true, "message", "Payment updated");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Map.of("success", false, "message", e.getMessage());
//        }
//    }
//
//    /** ✅ 3. Kiểm tra trạng thái thanh toán (Ajax) */
//    @PostMapping("/check-status")
//    public Map<String, Object> checkPaymentStatus(@RequestBody Map<String, Object> body) {
//        Long orderId = Long.parseLong(body.get("order_id").toString());
//        Map<String, Object> result = new HashMap<>();
//
//        PaymentOrder order = orderRepo.findById(orderId).orElse(null);
//        if (order == null) {
//            result.put("payment_status", "order_not_found");
//            return result;
//        }
//
//        // ✅ Khi đã Paid thì đảm bảo user được nâng cấp Premium
//        if ("Paid".equalsIgnoreCase(order.getPaymentStatus()) && order.getUser() != null) {
//            Long userId = order.getUser().getUserId();
//
//            userRepo.findById(userId).ifPresent(user -> {
//                if (!user.isPremium()) {
//                    user.setPremium(true);
//                    userRepo.save(user);
//                    System.out.println("⭐ User " + user.getEmail() + " đã được đồng bộ Premium!");
//                }
//
//                // 🔹 Kiểm tra đã log chưa (tránh duplicate)
//                boolean alreadyLogged = activityRepo.existsByActor_UserIdAndEntityTypeAndEntityIdAndAction(
//                        user.getUserId(), "PaymentOrder", order.getId(), "payment_success"
//                );
//
//                if (!alreadyLogged) {
//                    // Tạo log mới
//                    activityService.logWithActor(
//                            user.getUserId(),
//                            "PaymentOrder",
//                            order.getId(),
//                            "payment_success",
//                            String.format(
//                                    "{\"order_id\":%d,\"amount\":%.2f,\"plan\":\"%s\",\"message\":\"Thanh toán thành công, nâng cấp Premium.\"}",
//                                    order.getId(),
//                                    order.getTotal(),
//                                    order.getName() != null ? order.getName() : "Pro"
//                            )
//                    );
//
//                    // Gửi thông báo (đã có kiểm tra trùng trong notifyPaymentSuccess)
//                    notificationService.notifyPaymentSuccess(user, order);
//
//                    System.out.println("✅ Đã tạo Activity log PAYMENT_SUCCESS cho user " + user.getEmail());
//                } else {
//                    System.out.println("⚙️ Activity log PAYMENT_SUCCESS đã tồn tại, bỏ qua.");
//                }
//            });
//        }
//
//
//        result.put("payment_status", order.getPaymentStatus());
//        return result;
//    }
//}
package com.devcollab.controller.rest;

import com.devcollab.service.core.PaymentOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentOrderRestController {

    private final PaymentOrderService paymentOrderService;

    /** ➕ Tạo đơn hàng */
    @PostMapping("/create")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> body, Authentication auth) {
        String email = null;

        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.User userDetails) {
                // ✅ Local login
                email = userDetails.getUsername();
            } else if (principal instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User oauth2User) {
                // ✅ Google login
                email = (String) oauth2User.getAttribute("email");
            }
        }
        return paymentOrderService.createOrder(body, email);
    }

    /** 💳 Webhook từ SePay */
    @PostMapping("/webhook")
    public Map<String, Object> handleWebhook(@RequestBody Map<String, Object> payload) {
        return paymentOrderService.handleWebhook(payload);
    }

    /** 🔍 Kiểm tra trạng thái thanh toán */
    @PostMapping("/check-status")
    public Map<String, Object> checkStatus(@RequestBody Map<String, Object> body) {
        Long orderId = Long.parseLong(body.get("order_id").toString());
        return paymentOrderService.checkPaymentStatus(orderId);
    }
}
