package com.paytrix.cipherbank.application.port.out.business;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business.BankStatementUpload;

public interface BankStatementUploadRepositoryPort {
    BankStatementUpload save(BankStatementUpload upload);
}
