package com.paytrix.cipherbank.application.service;

import com.paytrix.cipherbank.application.port.in.PaymentVerificationUseCase;
import com.paytrix.cipherbank.application.port.out.business.BankStatementRepositoryPort;
import com.paytrix.cipherbank.domain.model.PaymentVerificationRequest;
import com.paytrix.cipherbank.domain.model.PaymentVerificationResponse;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business.BankStatement;
import com.paytrix.cipherbank.infrastructure.exception.MultipleRecordsFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for verifying payment details against bank statements
 * Implements priority-based matching logic with single-record enforcement
 * Only matches UNPROCESSED records (processed = false)
 *
 * NEW: Supports optional accountNo filtering
 * - If accountNo provided in request: Matches only within that account
 * - If accountNo NOT provided: Searches across all accounts (original behavior)
 *
 * NEW: Always returns 200 OK with response body
 * - Match found: Returns response with match flags true and DB values
 * - No match found: Returns response with all flags false, amt/accountNo null, but preserves request values
 *
 * Supports flexible matching:
 * - Both orderId and utr provided → Tries exact match, then orderId, then utr
 * - Only orderId provided → Matches by orderId only
 * - Only utr provided → Matches by utr only
 */
@Service
public class PaymentVerificationService implements PaymentVerificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(PaymentVerificationService.class);

    private final BankStatementRepositoryPort statementRepository;

    public PaymentVerificationService(BankStatementRepositoryPort statementRepository) {
        this.statementRepository = statementRepository;
    }

    @Override
    @Transactional
    public Optional<PaymentVerificationResponse> verifyPayment(PaymentVerificationRequest request) {

        boolean hasOrderId = request.getOrderId() != null && !request.getOrderId().trim().isEmpty();
        boolean hasUtr = request.getUtr() != null && !request.getUtr().trim().isEmpty();
        boolean hasAccountNo = request.getAccountNo() != null;

        log.info("Payment verification request received - GatewayTxnId: {}, OrderId: {}, UTR: {}, Amount: {}, AccountNo: {}",
                request.getGatewayTransactionId(),
                hasOrderId ? request.getOrderId() : "NOT_PROVIDED",
                hasUtr ? request.getUtr() : "NOT_PROVIDED",
                request.getAmt(),
                hasAccountNo ? request.getAccountNo() : "NOT_PROVIDED (search all accounts)");

        // CASE 1: Both orderId and utr provided
        if (hasOrderId && hasUtr) {
            return hasAccountNo
                    ? verifyWithBoth(request, request.getAccountNo())
                    : verifyWithBoth(request);
        }

        // CASE 2: Only orderId provided
        if (hasOrderId) {
            return hasAccountNo
                    ? verifyWithOrderIdOnly(request, request.getAccountNo())
                    : verifyWithOrderIdOnly(request);
        }

        // CASE 3: Only utr provided
        if (hasUtr) {
            return hasAccountNo
                    ? verifyWithUtrOnly(request, request.getAccountNo())
                    : verifyWithUtrOnly(request);
        }

        // CASE 4: Neither provided (should be caught by validation, but handle anyway)
        log.error("Neither orderId nor utr provided - should have been caught by validation");
        return Optional.of(buildNoMatchResponse(request));
    }

    // ========================================================================
    // CASE 1: Both orderId and utr provided - SEARCH ALL ACCOUNTS
    // ========================================================================

    /**
     * CASE 1: Both orderId and utr provided - Search across all accounts
     * Priority 1: Exact match (orderId + utr)
     * Priority 2: OrderId only match
     * Priority 3: UTR only match
     */
    private Optional<PaymentVerificationResponse> verifyWithBoth(PaymentVerificationRequest request) {
        log.info("Matching with BOTH orderId and utr (ALL ACCOUNTS)");

        // PRIORITY 1: Check for exact match (orderId + utr) in UNPROCESSED records
        List<BankStatement> exactMatches = statementRepository.findUnprocessedByOrderIdAndUtr(
                request.getOrderId(), request.getUtr());

        if (!exactMatches.isEmpty()) {
            log.info("Found {} unprocessed record(s) for exact match (orderId + utr): OrderId={}, UTR={}",
                    exactMatches.size(), request.getOrderId(), request.getUtr());

            validateSingleRecord(exactMatches, "orderId + utr",
                    String.format("OrderId='%s', UTR='%s'", request.getOrderId(), request.getUtr()));

            BankStatement statement = exactMatches.get(0);
            updateStatementWithGatewayTransaction(statement, request.getGatewayTransactionId());

            return Optional.of(buildResponse(request, statement, true, true));
        }

        // PRIORITY 2: Check for orderId match only in UNPROCESSED records
        List<BankStatement> orderIdMatches = statementRepository.findUnprocessedByOrderId(request.getOrderId());

        if (!orderIdMatches.isEmpty()) {
            log.info("Found {} unprocessed record(s) for orderId match: OrderId={} (UTR mismatch)",
                    orderIdMatches.size(), request.getOrderId());

            validateSingleRecord(orderIdMatches, "orderId",
                    String.format("OrderId='%s'", request.getOrderId()));

            BankStatement statement = orderIdMatches.get(0);
            updateStatementWithGatewayTransaction(statement, request.getGatewayTransactionId());

            return Optional.of(buildResponse(request, statement, true, false));
        }

        // PRIORITY 3: Check for utr match only in UNPROCESSED records
        List<BankStatement> utrMatches = statementRepository.findUnprocessedByUtr(request.getUtr());

        if (!utrMatches.isEmpty()) {
            log.info("Found {} unprocessed record(s) for UTR match: UTR={} (OrderId mismatch)",
                    utrMatches.size(), request.getUtr());

            validateSingleRecord(utrMatches, "utr",
                    String.format("UTR='%s'", request.getUtr()));

            BankStatement statement = utrMatches.get(0);
            updateStatementWithGatewayTransaction(statement, request.getGatewayTransactionId());

            return Optional.of(buildResponse(request, statement, false, true));
        }

        // No unprocessed match found - Return "no match" response
        log.warn("No unprocessed match found for OrderId: {}, UTR: {} (ALL ACCOUNTS)",
                request.getOrderId(), request.getUtr());
        return Optional.of(buildNoMatchResponse(request));
    }

    // ========================================================================
    // CASE 1: Both orderId and utr provided - SPECIFIC ACCOUNT
    // ========================================================================

    /**
     * CASE 1: Both orderId and utr provided - Filter by specific account
     * Priority 1: Exact match (orderId + utr) for this account
     * Priority 2: OrderId only match for this account
     * Priority 3: UTR only match for this account
     */
    private Optional<PaymentVerificationResponse> verifyWithBoth(
            PaymentVerificationRequest request, Long accountNo) {

        log.info("Matching with BOTH orderId and utr (ACCOUNT {})", accountNo);

        // PRIORITY 1: Check for exact match (orderId + utr) for this account
        List<BankStatement> exactMatches = statementRepository.findUnprocessedByOrderIdAndUtr(
                request.getOrderId(), request.getUtr(), accountNo);

        if (!exactMatches.isEmpty()) {
            log.info("Found {} unprocessed record(s) for exact match (orderId + utr) in Account {}: OrderId={}, UTR={}",
                    exactMatches.size(), accountNo, request.getOrderId(), request.getUtr());

            validateSingleRecord(exactMatches, "orderId + utr",
                    String.format("OrderId='%s', UTR='%s', AccountNo=%d",
                            request.getOrderId(), request.getUtr(), accountNo));

            BankStatement statement = exactMatches.get(0);
            updateStatementWithGatewayTransaction(statement, request.getGatewayTransactionId());

            return Optional.of(buildResponse(request, statement, true, true));
        }

        // PRIORITY 2: Check for orderId match only for this account
        List<BankStatement> orderIdMatches = statementRepository.findUnprocessedByOrderId(
                request.getOrderId(), accountNo);

        if (!orderIdMatches.isEmpty()) {
            log.info("Found {} unprocessed record(s) for orderId match in Account {}: OrderId={} (UTR mismatch)",
                    orderIdMatches.size(), accountNo, request.getOrderId());

            validateSingleRecord(orderIdMatches, "orderId",
                    String.format("OrderId='%s', AccountNo=%d", request.getOrderId(), accountNo));

            BankStatement statement = orderIdMatches.get(0);
            updateStatementWithGatewayTransaction(statement, request.getGatewayTransactionId());

            return Optional.of(buildResponse(request, statement, true, false));
        }

        // PRIORITY 3: Check for utr match only for this account
        List<BankStatement> utrMatches = statementRepository.findUnprocessedByUtr(
                request.getUtr(), accountNo);

        if (!utrMatches.isEmpty()) {
            log.info("Found {} unprocessed record(s) for UTR match in Account {}: UTR={} (OrderId mismatch)",
                    utrMatches.size(), accountNo, request.getUtr());

            validateSingleRecord(utrMatches, "utr",
                    String.format("UTR='%s', AccountNo=%d", request.getUtr(), accountNo));

            BankStatement statement = utrMatches.get(0);
            updateStatementWithGatewayTransaction(statement, request.getGatewayTransactionId());

            return Optional.of(buildResponse(request, statement, false, true));
        }

        // No unprocessed match found for this account - Return "no match" response
        log.warn("No unprocessed match found for OrderId: {}, UTR: {} in Account {}",
                request.getOrderId(), request.getUtr(), accountNo);
        return Optional.of(buildNoMatchResponse(request));
    }

    // ========================================================================
    // CASE 2: Only orderId provided - SEARCH ALL ACCOUNTS
    // ========================================================================

    /**
     * CASE 2: Only orderId provided - Search across all accounts
     * Matches by orderId only
     */
    private Optional<PaymentVerificationResponse> verifyWithOrderIdOnly(PaymentVerificationRequest request) {
        log.info("Matching with ONLY orderId (no UTR provided) (ALL ACCOUNTS)");

        List<BankStatement> orderIdMatches = statementRepository.findUnprocessedByOrderId(request.getOrderId());

        if (!orderIdMatches.isEmpty()) {
            log.info("Found {} unprocessed record(s) for orderId: OrderId={}",
                    orderIdMatches.size(), request.getOrderId());

            validateSingleRecord(orderIdMatches, "orderId",
                    String.format("OrderId='%s'", request.getOrderId()));

            BankStatement statement = orderIdMatches.get(0);
            updateStatementWithGatewayTransaction(statement, request.getGatewayTransactionId());

            // orderIdMatch=true, utrMatch=false (utr not provided in request)
            return Optional.of(buildResponse(request, statement, true, false));
        }

        log.warn("No unprocessed match found for OrderId: {} (ALL ACCOUNTS)", request.getOrderId());
        return Optional.of(buildNoMatchResponse(request));
    }

    // ========================================================================
    // CASE 2: Only orderId provided - SPECIFIC ACCOUNT
    // ========================================================================

    /**
     * CASE 2: Only orderId provided - Filter by specific account
     * Matches by orderId for this account
     */
    private Optional<PaymentVerificationResponse> verifyWithOrderIdOnly(
            PaymentVerificationRequest request, Long accountNo) {

        log.info("Matching with ONLY orderId (no UTR provided) (ACCOUNT {})", accountNo);

        List<BankStatement> orderIdMatches = statementRepository.findUnprocessedByOrderId(
                request.getOrderId(), accountNo);

        if (!orderIdMatches.isEmpty()) {
            log.info("Found {} unprocessed record(s) for orderId in Account {}: OrderId={}",
                    orderIdMatches.size(), accountNo, request.getOrderId());

            validateSingleRecord(orderIdMatches, "orderId",
                    String.format("OrderId='%s', AccountNo=%d", request.getOrderId(), accountNo));

            BankStatement statement = orderIdMatches.get(0);
            updateStatementWithGatewayTransaction(statement, request.getGatewayTransactionId());

            // orderIdMatch=true, utrMatch=false (utr not provided in request)
            return Optional.of(buildResponse(request, statement, true, false));
        }

        log.warn("No unprocessed match found for OrderId: {} in Account {}",
                request.getOrderId(), accountNo);
        return Optional.of(buildNoMatchResponse(request));
    }

    // ========================================================================
    // CASE 3: Only utr provided - SEARCH ALL ACCOUNTS
    // ========================================================================

    /**
     * CASE 3: Only utr provided - Search across all accounts
     * Matches by utr only
     */
    private Optional<PaymentVerificationResponse> verifyWithUtrOnly(PaymentVerificationRequest request) {
        log.info("Matching with ONLY utr (no orderId provided) (ALL ACCOUNTS)");

        List<BankStatement> utrMatches = statementRepository.findUnprocessedByUtr(request.getUtr());

        if (!utrMatches.isEmpty()) {
            log.info("Found {} unprocessed record(s) for UTR: UTR={}",
                    utrMatches.size(), request.getUtr());

            validateSingleRecord(utrMatches, "utr",
                    String.format("UTR='%s'", request.getUtr()));

            BankStatement statement = utrMatches.get(0);
            updateStatementWithGatewayTransaction(statement, request.getGatewayTransactionId());

            // orderIdMatch=false (orderId not provided in request), utrMatch=true
            return Optional.of(buildResponse(request, statement, false, true));
        }

        log.warn("No unprocessed match found for UTR: {} (ALL ACCOUNTS)", request.getUtr());
        return Optional.of(buildNoMatchResponse(request));
    }

    // ========================================================================
    // CASE 3: Only utr provided - SPECIFIC ACCOUNT
    // ========================================================================

    /**
     * CASE 3: Only utr provided - Filter by specific account
     * Matches by utr for this account
     */
    private Optional<PaymentVerificationResponse> verifyWithUtrOnly(
            PaymentVerificationRequest request, Long accountNo) {

        log.info("Matching with ONLY utr (no orderId provided) (ACCOUNT {})", accountNo);

        List<BankStatement> utrMatches = statementRepository.findUnprocessedByUtr(
                request.getUtr(), accountNo);

        if (!utrMatches.isEmpty()) {
            log.info("Found {} unprocessed record(s) for UTR in Account {}: UTR={}",
                    utrMatches.size(), accountNo, request.getUtr());

            validateSingleRecord(utrMatches, "utr",
                    String.format("UTR='%s', AccountNo=%d", request.getUtr(), accountNo));

            BankStatement statement = utrMatches.get(0);
            updateStatementWithGatewayTransaction(statement, request.getGatewayTransactionId());

            // orderIdMatch=false (orderId not provided in request), utrMatch=true
            return Optional.of(buildResponse(request, statement, false, true));
        }

        log.warn("No unprocessed match found for UTR: {} in Account {}",
                request.getUtr(), accountNo);
        return Optional.of(buildNoMatchResponse(request));
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Validate that exactly one record was found
     * Throws exception if multiple records found (data inconsistency)
     */
    private void validateSingleRecord(List<BankStatement> records, String matchType, String criteria) {
        if (records.size() > 1) {
            log.error("DATA INCONSISTENCY: Found {} unprocessed records matching {} for {}",
                    records.size(), matchType, criteria);
            throw new MultipleRecordsFoundException(records.size(), matchType, criteria);
        }
    }

    /**
     * Update the matched bank statement with gateway transaction ID and mark as processed
     */
    private void updateStatementWithGatewayTransaction(BankStatement statement, Long gatewayTransactionId) {
        log.info("Updating statement ID {} with gatewayTransactionId={}, marking as processed",
                statement.getId(), gatewayTransactionId);

        // Double-check it's not already processed (should never happen with our queries)
        if (statement.isProcessed()) {
            log.error("UNEXPECTED: Statement ID {} is already processed but was returned by unprocessed query!",
                    statement.getId());
        }

        // Update fields
        statement.setGatewayTransactionId(gatewayTransactionId);
        statement.setProcessed(true);

        // Save updated statement
        BankStatement updated = statementRepository.save(statement);

        if (updated != null) {
            log.info("Successfully updated statement ID {} - gatewayTransactionId={}, processed=true",
                    updated.getId(), gatewayTransactionId);
        } else {
            log.error("Failed to update statement ID {}", statement.getId());
        }
    }

    /**
     * Build verification response when match is found
     * AccountNo comes from database (not from request)
     */
    private PaymentVerificationResponse buildResponse(
            PaymentVerificationRequest request,
            BankStatement statement,
            boolean orderIdMatch,
            boolean utrMatch) {

        // Compare amounts (use compareTo for BigDecimal comparison)
        boolean amtMatch = statement.getAmount() != null &&
                statement.getAmount().compareTo(request.getAmt()) == 0;

        if (!amtMatch) {
            log.warn("Amount mismatch - Request: {}, DB: {}", request.getAmt(), statement.getAmount());
        }

        return PaymentVerificationResponse.builder()
                .gatewayTransactionId(request.getGatewayTransactionId())
                .orderId(request.getOrderId() != null ? request.getOrderId() : statement.getOrderId())  // Use DB if not in request
                .utr(request.getUtr() != null ? request.getUtr() : statement.getUtr())                  // Use DB if not in request
                .accountNo(statement.getAccountNo())  // Always from DB
                .amt(statement.getAmount())           // Always from DB
                .orderIdMatch(orderIdMatch)
                .utrMatch(utrMatch)
                .amtMatch(amtMatch)
                .build();
    }

    /**
     * Build verification response when NO match is found
     * All match flags are false, amt and accountNo are null
     * But orderId, utr, and gatewayTransactionId are preserved from request
     */
    private PaymentVerificationResponse buildNoMatchResponse(PaymentVerificationRequest request) {
        log.info("Building 'no match' response");

        return PaymentVerificationResponse.builder()
                .gatewayTransactionId(request.getGatewayTransactionId())  // From request
                .orderId(request.getOrderId())                            // From request (may be null)
                .utr(request.getUtr())                                    // From request (may be null)
                .accountNo(null)                                          // NULL - no match found
                .amt(null)                                                // NULL - no match found
                .orderIdMatch(false)                                      // FALSE - no match
                .utrMatch(false)                                          // FALSE - no match
                .amtMatch(false)                                          // FALSE - no match
                .build();
    }
}