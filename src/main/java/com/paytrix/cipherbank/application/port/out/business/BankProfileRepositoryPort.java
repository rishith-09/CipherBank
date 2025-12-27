package com.paytrix.cipherbank.application.port.out.business;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business.BankProfile;
import java.util.Optional;

public interface BankProfileRepositoryPort {
    Optional<BankProfile> findByParserKey(String parserKey);
}
