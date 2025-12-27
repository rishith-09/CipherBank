package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.adapter.security;

import com.paytrix.cipherbank.application.port.out.security.IpWhitelistRepositoryPort;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.IpWhitelist;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.security.JpaIpWhitelistRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IpWhitelistRepositoryAdapter implements IpWhitelistRepositoryPort {

    private final JpaIpWhitelistRepository jpaRepository;

    public IpWhitelistRepositoryAdapter(JpaIpWhitelistRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<IpWhitelist> findAllActive() {
        return jpaRepository.findAllByActiveTrue();
    }
}