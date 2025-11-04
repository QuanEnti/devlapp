package com.devcollab.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "Transaction") // ✅ Không cần escape bằng dấu " để tránh lỗi
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id", nullable = false)
    private Long id;

    // ✅ Liên kết với PaymentOrder, nếu PaymentOrder bị xóa thì set null
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "order_id", foreignKey = @ForeignKey(name = "FK_Transaction_Order"))
    private PaymentOrder order;

    @Size(max = 100)
    @NotNull
    @Nationalized
    @Column(name = "gateway", nullable = false, length = 100)
    private String gateway;

    // ✅ Chuẩn Hibernate 6: sử dụng datetimeoffset(6) + SYSDATETIMEOFFSET()
    @NotNull
    @ColumnDefault("SYSDATETIMEOFFSET()")
    @Column(name = "transaction_date", nullable = false, columnDefinition = "datetimeoffset(6)")
    private Instant transactionDate;

    @Size(max = 100)
    @Nationalized
    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Size(max = 250)
    @Nationalized
    @Column(name = "sub_account", length = 250)
    private String subAccount;

    @NotNull
    @ColumnDefault("0.00")
    @Column(name = "amount_in", nullable = false, precision = 20, scale = 2)
    private BigDecimal amountIn = BigDecimal.ZERO;

    @NotNull
    @ColumnDefault("0.00")
    @Column(name = "amount_out", nullable = false, precision = 20, scale = 2)
    private BigDecimal amountOut = BigDecimal.ZERO;

    @NotNull
    @ColumnDefault("0.00")
    @Column(name = "accumulated", nullable = false, precision = 20, scale = 2)
    private BigDecimal accumulated = BigDecimal.ZERO;

    @Size(max = 250)
    @Nationalized
    @Column(name = "code", length = 250)
    private String code;

    @Lob
    @Nationalized
    @Column(name = "transaction_content")
    private String transactionContent;

    @Size(max = 255)
    @Nationalized
    @Column(name = "reference_number")
    private String referenceNumber;

    @Lob
    @Nationalized
    @Column(name = "body")
    private String body;

    // ✅ Sử dụng datetimeoffset(6) để khớp Hibernate Instant
    @NotNull
    @ColumnDefault("SYSDATETIMEOFFSET()")
    @Column(name = "created_at", nullable = false, columnDefinition = "datetimeoffset(6)")
    private Instant createdAt;
}
