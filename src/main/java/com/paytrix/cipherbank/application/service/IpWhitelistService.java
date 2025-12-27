package com.paytrix.cipherbank.application.service;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.IpWhitelist;
import com.paytrix.cipherbank.application.port.out.security.IpWhitelistRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IpWhitelistService {

    private final IpWhitelistRepositoryPort repositoryPort;

    public IpWhitelistService(IpWhitelistRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public Set<String> getActiveIps() {

        return repositoryPort.findAllActive()
                .stream()
                .map(IpWhitelist::getIpAddress)
                .collect(Collectors.toSet());
    }
}
