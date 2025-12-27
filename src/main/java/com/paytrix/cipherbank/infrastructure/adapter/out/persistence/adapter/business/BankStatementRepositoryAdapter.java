package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.adapter.business;

import com.paytrix.cipherbank.application.port.out.business.BankStatementRepositoryPort;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business.BankStatement;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.business.JpaBankStatementRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BankStatementRepositoryAdapter implements BankStatementRepositoryPort {

    private final JpaBankStatementRepository jpa;

    public BankStatementRepositoryAdapter(JpaBankStatementRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public BankStatement save(BankStatement stmt) {
        try {
            return jpa.saveAndFlush(stmt);
        } catch (DataIntegrityViolationException dup) {
            // Likely unique key violation (dedupe). Caller can count it as deduped.
            return null;
        }
    }

    @Override
    public boolean existsByUtrAndOrderIdAndAccountNo(String utr, String orderId, Long accountNo) {
        return jpa.existsByUtrAndOrderIdAndAccountNo(utr, orderId, accountNo);
    }

    @Override
    public List<BankStatement> findUnprocessedByOrderIdAndUtr(String orderId, String utr) {
        // Only find records where processed = false (0)
        return jpa.findByOrderIdAndUtrAndProcessed(orderId, utr, false);
    }

    @Override
    public List<BankStatement> findUnprocessedByOrderId(String orderId) {
        // Only find records where processed = false (0)
        return jpa.findByOrderIdAndProcessed(orderId, false);
    }

    @Override
    public List<BankStatement> findUnprocessedByUtr(String utr) {
        // Only find records where processed = false (0)
        return jpa.findByUtrAndProcessed(utr, false);
    }
}