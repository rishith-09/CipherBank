package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.adapter.business;

import com.paytrix.cipherbank.application.port.out.business.BankStatementRepositoryPort;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business.BankStatement;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.business.JpaBankStatementRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BankStatementRepositoryAdapter implements BankStatementRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(BankStatementRepositoryAdapter.class);

    private final JpaBankStatementRepository jpa;

    @PersistenceContext(unitName = "businessEntityManagerFactory")
    private EntityManager entityManager;

    public BankStatementRepositoryAdapter(JpaBankStatementRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public BankStatement save(BankStatement stmt) {
        try {
            return jpa.saveAndFlush(stmt);
        } catch (DataIntegrityViolationException dup) {
            log.debug("Duplicate detected during save - UTR={}, OrderID={}, AccountNo={}. " +
                            "Entity will be detached from session.",
                    stmt.getUtr(), stmt.getOrderId(), stmt.getAccountNo());

            // CRITICAL FIX: Detach failed entity from Hibernate session
            // This prevents "null identifier" errors on subsequent operations
            if (entityManager != null && entityManager.contains(stmt)) {
                entityManager.detach(stmt);
                log.debug("Successfully detached failed entity from Hibernate session");
            }

            // Return null to indicate duplicate (caller counts as deduped)
            return null;
        } catch (Exception ex) {
            // Also detach on other exceptions to keep session clean
            if (entityManager != null && entityManager.contains(stmt)) {
                entityManager.detach(stmt);
                log.warn("Detached entity after exception: {}", ex.getMessage());
            }
            throw ex;
        }
    }

    @Override
    @Deprecated
    public boolean existsByUtrAndOrderIdAndAccountNo(String utr, String orderId, Long accountNo) {
        log.warn("DEPRECATED METHOD CALLED: existsByUtrAndOrderIdAndAccountNo() - Use existsByAccountNoAndUtr() instead");
        return jpa.existsByUtrAndOrderIdAndAccountNo(utr, orderId, accountNo);
    }

    @Override
    public boolean existsByAccountNoAndUtr(Long accountNo, String utr) {
        // This is the CORRECT method - matches constraint uk_stmt_acct_utr
        return jpa.existsByAccountNoAndUtr(accountNo, utr);
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