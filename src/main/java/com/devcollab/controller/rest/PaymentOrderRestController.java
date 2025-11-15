package com.devcollab.controller.rest;

import com.devcollab.service.core.PaymentOrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentOrderRestController {

    private final PaymentOrderService paymentOrderService;

    /** ➕ Tạo đơn hàng */
    @PostMapping("/create")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> body,
            Authentication auth) {
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

    @PostMapping(value = "/webhook", consumes = MediaType.ALL_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) throws IOException {

        String raw = request.getReader().lines().collect(Collectors.joining());
        System.out.println("📩 RAW WEBHOOK: " + raw);

        return ResponseEntity.ok("OK");
    }


    /** 🔍 Kiểm tra trạng thái thanh toán */
    @PostMapping("/check-status")
    public Map<String, Object> checkStatus(@RequestBody Map<String, Object> body) {
        Long orderId = Long.parseLong(body.get("order_id").toString());
        return paymentOrderService.checkPaymentStatus(orderId);
    }
}
