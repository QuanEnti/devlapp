package com.devcollab.service.core;

import java.util.Map;

public interface PaymentOrderService {

    /** Tạo đơn hàng mới (mua gói Pro) */
    Map<String, Object> createOrder(Map<String, Object> body, String userEmail);

    /** Xử lý webhook từ SePay */
    Map<String, Object> handleWebhook(Map<String, Object> payload);

    /** Kiểm tra trạng thái thanh toán */
    Map<String, Object> checkPaymentStatus(Long orderId);
}
