package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@Table(name = "bank_statement_uploads",
        indexes = {
                @Index(name = "idx_upload_time", columnList = "upload_time"),
                @Index(name = "idx_upload_bank_id", columnList = "bank_id")
        })
public class BankStatementUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upload_time", nullable = false)
    private Instant uploadTime;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bank_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_upload_bank")
    )
    private BankProfile bank;

    @OneToMany(mappedBy = "upload")
    private Set<BankStatement> statements;

    @PrePersist
    void prePersist() {

        if (uploadTime == null)
            uploadTime = Instant.now();
    }
}
