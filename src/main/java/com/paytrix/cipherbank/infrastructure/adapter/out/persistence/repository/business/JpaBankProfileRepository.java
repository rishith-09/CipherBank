package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.business;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business.BankProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaBankProfileRepository extends JpaRepository<BankProfile, Long> {
    Optional<BankProfile> findByParserKey(String parserKey);
}
