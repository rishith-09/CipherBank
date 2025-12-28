package com.paytrix.cipherbank.application.port.out.business;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business.BankStatement;

import java.util.List;

public interface BankStatementRepositoryPort {
    BankStatement save(BankStatement stmt);

    /**
     * Check if a duplicate statement already exists
     * A duplicate is defined as having the same combination of:
     * - UTR (Unique Transaction Reference)
     * - Order ID
     * - Account Number
     *
     * @param utr Transaction reference number
     * @param orderId Order/transaction ID
     * @param accountNo Account number
     * @return true if duplicate exists, false otherwise
     */
    boolean existsByUtrAndOrderIdAndAccountNo(String utr, String orderId, Long accountNo);

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

    // NEW METHODS WITH ACCOUNT NUMBER FILTER

    /**
     * Find ALL unprocessed statements matching orderId and utr for specific account
     * Only returns records where processed = false and accountNo matches
     * Returns list to detect multiple matches (data inconsistency)
     *
     * @param orderId Order ID
     * @param utr Unique Transaction Reference
     * @param accountNo Account Number to filter by
     * @return List of unprocessed matching statements for this account
     */
    List<BankStatement> findUnprocessedByOrderIdAndUtr(String orderId, String utr, Long accountNo);

    /**
     * Find ALL unprocessed statements matching orderId for specific account
     * Only returns records where processed = false and accountNo matches
     * Returns list to detect multiple matches (data inconsistency)
     *
     * @param orderId Order ID
     * @param accountNo Account Number to filter by
     * @return List of unprocessed matching statements for this account
     */
    List<BankStatement> findUnprocessedByOrderId(String orderId, Long accountNo);

    /**
     * Find ALL unprocessed statements matching utr for specific account
     * Only returns records where processed = false and accountNo matches
     * Returns list to detect multiple matches (data inconsistency)
     *
     * @param utr Unique Transaction Reference
     * @param accountNo Account Number to filter by
     * @return List of unprocessed matching statements for this account
     */
    List<BankStatement> findUnprocessedByUtr(String utr, Long accountNo);
}