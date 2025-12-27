package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.adapter.business;

import com.paytrix.cipherbank.application.port.out.business.BankProfileRepositoryPort;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business.BankProfile;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.business.JpaBankProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BankProfileRepositoryAdapter implements BankProfileRepositoryPort {

    private final JpaBankProfileRepository jpa;

    public BankProfileRepositoryAdapter(JpaBankProfileRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<BankProfile> findByParserKey(String parserKey) {
        return jpa.findByParserKey(parserKey);
    }
}
