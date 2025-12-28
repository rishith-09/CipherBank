package com.paytrix.cipherbank.application.port.out.business;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business.BankStatement;

import java.util.List;

public interface BankStatementRepositoryPort {
    BankStatement save(BankStatement stmt);

    /**
     * DEPRECATED: This method checks the 3-column constraint (utr, order_id, account_no)
     * but doesn't prevent violations of the 2-column constraint (account_no, utr).
     * Use existsByAccountNoAndUtr() instead for proper deduplication.
     *
     * @deprecated Use {@link #existsByAccountNoAndUtr(Long, String)} instead
     */
    @Deprecated
    boolean existsByUtrAndOrderIdAndAccountNo(String utr, String orderId, Long accountNo);

    /**
     * Check if a statement exists with the same account number and UTR.
     * This matches the database constraint uk_stmt_acct_utr (account_no, utr).
     *
     * CRITICAL: This is the correct deduplication check to use because:
     * - Database constraint #2 enforces uniqueness on (account_no, utr) only
     * - Same UTR can appear multiple times with different order_ids
     * - This prevents DataIntegrityViolationException at save time
     *
     * @param accountNo Account number
     * @param utr Unique Transaction Reference
     * @return true if duplicate exists, false otherwise
     */
    boolean existsByAccountNoAndUtr(Long accountNo, String utr);

    /**
     * Find ALL unprocessed statements matching orderId and utr combination
     * Only returns records where processed = false
     * Returns list to detect multiple matches (data inconsistency)
     *
     * @param orderId Order ID
     * @param utr Unique Transaction Reference
     * @return List of unprocessed matching statements
     */
    List<BankStatement> findUnprocessedByOrderIdAndUtr(String orderId, String utr);

    /**
     * Find ALL unprocessed statements matching orderId only
     * Only returns records where processed = false
     * Returns list to detect multiple matches (data inconsistency)
     *
     * @param orderId Order ID
     * @return List of unprocessed matching statements
     */
    List<BankStatement> findUnprocessedByOrderId(String orderId);

    /**
     * Find ALL unprocessed statements matching utr only
     * Only returns records where processed = false
     * Returns list to detect multiple matches (data inconsistency)
     *
     * @param utr Unique Transaction Reference
     * @return List of unprocessed matching statements
     */
    List<BankStatement> findUnprocessedByUtr(String utr);
}