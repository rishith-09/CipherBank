package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.business;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business.BankStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaBankStatementRepository extends JpaRepository<BankStatement, Long> {

    /**
     * DEPRECATED: Use existsByAccountNoAndUtr instead
     * @deprecated Checks wrong constraint - use existsByAccountNoAndUtr
     */
    @Deprecated
    boolean existsByUtrAndOrderIdAndAccountNo(String utr, String orderId, Long accountNo);

    /**
     * Check if statement exists with same account number and UTR
     * Matches database constraint uk_stmt_acct_utr (account_no, utr)
     * THIS IS THE CORRECT METHOD FOR DEDUPLICATION
     *
     * @param accountNo Account number
     * @param utr Unique Transaction Reference
     * @return true if exists, false otherwise
     */
    boolean existsByAccountNoAndUtr(Long accountNo, String utr);

    /**
     * Find ALL unprocessed statements matching orderId and utr (PRIORITY 1)
     * Only returns records where processed = false (0)
     * Returns list to detect multiple matches (data inconsistency)
     *
     * @param orderId Order ID
     * @param utr Unique Transaction Reference
     * @param processed Process status (should be false)
     * @return List of unprocessed matching statements
     */
    List<BankStatement> findByOrderIdAndUtrAndProcessed(String orderId, String utr, boolean processed);

    /**
     * Find ALL unprocessed statements matching orderId only (PRIORITY 2)
     * Only returns records where processed = false (0)
     * Returns list to detect multiple matches (data inconsistency)
     *
     * @param orderId Order ID
     * @param processed Process status (should be false)
     * @return List of unprocessed matching statements
     */
    List<BankStatement> findByOrderIdAndProcessed(String orderId, boolean processed);

    /**
     * Find ALL unprocessed statements matching utr only (PRIORITY 3)
     * Only returns records where processed = false (0)
     * Returns list to detect multiple matches (data inconsistency)
     *
     * @param utr Unique Transaction Reference
     * @param processed Process status (should be false)
     * @return List of unprocessed matching statements
     */
    List<BankStatement> findByUtrAndProcessed(String utr, boolean processed);
}