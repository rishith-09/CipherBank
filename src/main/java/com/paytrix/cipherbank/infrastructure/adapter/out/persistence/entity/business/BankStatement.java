package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "bank_statements",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stmt_upload_utr", columnNames = {"bank_upload_id", "utr"}),
                @UniqueConstraint(name = "uk_stmt_acct_utr", columnNames = {"account_no", "utr"})
        },
        indexes = {
                @Index(name = "idx_stmt_txn_dt", columnList = "transaction_date_time"),
                @Index(name = "idx_stmt_acct", columnList = "account_no"),
                @Index(name = "idx_stmt_utr", columnList = "utr")
        })
public class BankStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_date_time", nullable = false)
    private LocalDateTime transactionDateTime;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "balance", precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "reference")
    private String reference;

    @Column(name = "pay_in", nullable = false)
    private boolean payIn;

    @Column(name = "account_no")
    private Long accountNo;

    @Column(name = "utr", length = 12)
    private String utr;

    @Column(name = "gateway_transaction_id")
    private Long gatewayTransactionId;

    @Column(name = "upload_timestamp", nullable = false)
    private Instant uploadTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bank_upload_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stmt_upload"))
    private BankStatementUpload upload;

    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    @Column(name = "type")
    private String type = orderId;

    @PrePersist
    void prePersist() {

        if (uploadTimestamp == null)
            uploadTimestamp = Instant.now();
    }
}