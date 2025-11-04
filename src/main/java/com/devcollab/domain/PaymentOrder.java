package com.devcollab.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
public class PaymentOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false)
    private Long id;

    @NotNull
    @ColumnDefault("0.00")
    @Column(name = "total", nullable = false, precision = 20, scale = 2)
    private BigDecimal total;

    @Size(max = 20)
    @NotNull
    @Nationalized
    @ColumnDefault("'Unpaid'")
    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus;

    @Size(max = 250)
    @Nationalized
    @Column(name = "name", length = 250)
    private String name;

    @NotNull
    @ColumnDefault("sysdatetime()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", foreignKey = @ForeignKey(name = "FK_PaymentOrder_User"))
    private User user;
}