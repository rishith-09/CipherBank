package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.business;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business.BankStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaBankStatementRepository extends JpaRepository<BankStatement, Long> {

    /**
     * Check if a statement already exists with the given UTR, Order ID, and Account Number
     * Used for deduplication before inserting new statements
     *
     * @param utr Unique Transaction Reference
     * @param orderId Order/Transaction ID
     * @param accountNo Account Number
     * @return true if a matching record exists, false otherwise
     */
    boolean existsByUtrAndOrderIdAndAccountNo(String utr, String orderId, Long accountNo);

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

    // NEW METHODS WITH ACCOUNT NUMBER FILTER

    /**
     * Find ALL unprocessed statements matching orderId and utr for specific account
     * Only returns records where processed = false and accountNo matches
     * Returns list to detect multiple matches (data inconsistency)
     *
     * @param orderId Order ID
     * @param utr Unique Transaction Reference
     * @param processed Process status (should be false)
     * @param accountNo Account Number
     * @return List of unprocessed matching statements for this account
     */
    List<BankStatement> findByOrderIdAndUtrAndProcessedAndAccountNo(
            String orderId, String utr, boolean processed, Long accountNo);

    /**
     * Find ALL unprocessed statements matching orderId for specific account
     * Only returns records where processed = false and accountNo matches
     * Returns list to detect multiple matches (data inconsistency)
     *
     * @param orderId Order ID
     * @param processed Process status (should be false)
     * @param accountNo Account Number
     * @return List of unprocessed matching statements for this account
     */
    List<BankStatement> findByOrderIdAndProcessedAndAccountNo(
            String orderId, boolean processed, Long accountNo);

    /**
     * Find ALL unprocessed statements matching utr for specific account
     * Only returns records where processed = false and accountNo matches
     * Returns list to detect multiple matches (data inconsistency)
     *
     * @param utr Unique Transaction Reference
     * @param processed Process status (should be false)
     * @param accountNo Account Number
     * @return List of unprocessed matching statements for this account
     */
    List<BankStatement> findByUtrAndProcessedAndAccountNo(
            String utr, boolean processed, Long accountNo);
}