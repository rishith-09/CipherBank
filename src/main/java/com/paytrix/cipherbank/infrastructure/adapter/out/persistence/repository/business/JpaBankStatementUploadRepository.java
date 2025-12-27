package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.business;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business.BankStatementUpload;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaBankStatementUploadRepository extends JpaRepository<BankStatementUpload, Long> {
}
