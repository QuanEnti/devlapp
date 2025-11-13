package com.devcollab.repository;

import com.devcollab.domain.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    @Query("""
        SELECT FORMAT(p.createdAt, 'yyyy-MM') AS month, SUM(p.total)
        FROM PaymentOrder p
        WHERE p.paymentStatus = 'Paid'
        GROUP BY FORMAT(p.createdAt, 'yyyy-MM')
        ORDER BY month
    """)
    List<Object[]> sumRevenueByMonth();
}
