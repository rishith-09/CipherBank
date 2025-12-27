package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ip_whitelist")
@Data
@NoArgsConstructor
public class IpWhitelist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_address", nullable = false, unique = true)
    private String ipAddress;

    @Column(nullable = false)
    private boolean active;
}
