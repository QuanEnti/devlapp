package com.devcollab.controller.view;

import com.devcollab.domain.PaymentOrder;
import com.devcollab.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PaymentOrderViewController {

    private final PaymentOrderRepository orderRepo;

    @GetMapping("/payment/checkout")
    public String checkoutPage(@RequestParam("id") Long orderId, Model model) {
        PaymentOrder order = orderRepo.findById(orderId).orElse(null);
        if (order == null) {
            model.addAttribute("error", "Không tìm thấy đơn hàng!");
            return "payment/error";
        }
        model.addAttribute("order", order);
        return "payment/checkout";
    }
}
