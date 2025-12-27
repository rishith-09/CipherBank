package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@Table(name = "bank_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bank_profiles_name", columnNames = {"name"}),
                @UniqueConstraint(name = "uk_bank_profiles_parser_key", columnNames = {"parser_key"})
        },
        indexes = @Index(name = "idx_bank_profiles_name", columnList = "name"))
public class BankProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "parser_key", nullable = false, length = 64)
    private String parserKey;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "pdf_password_enabled", nullable = false)
    private boolean pdfPasswordEnabled = false;

    @Column(name = "account_number_required", nullable = false)
    private boolean accountNumberRequired = false;

    @OneToMany(mappedBy = "bank")
    private Set<BankStatementUpload> uploads;
}
