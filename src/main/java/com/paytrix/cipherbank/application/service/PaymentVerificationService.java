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

        log.info("Payment verification request received - GatewayTxnId: {}, OrderId: {}, UTR: {}, Amount: {}",
                request.getGatewayTransactionId(),
                hasOrderId ? request.getOrderId() : "NOT_PROVIDED",
                hasUtr ? request.getUtr() : "NOT_PROVIDED",
                request.getAmt());

        // CASE 1: Both orderId and utr provided
        if (hasOrderId && hasUtr) {
            return verifyWithBoth(request);
        }

        // CASE 2: Only orderId provided
        if (hasOrderId) {
            return verifyWithOrderIdOnly(request);
        }

        // CASE 3: Only utr provided
        if (hasUtr) {
            return verifyWithUtrOnly(request);
        }

        // CASE 4: Neither provided (should be caught by validation, but handle anyway)
        log.error("Neither orderId nor utr provided - should have been caught by validation");
        return Optional.empty();
    }

    /**
     * CASE 1: Both orderId and utr provided
     * Priority 1: Exact match (orderId + utr)
     * Priority 2: OrderId only match
     * Priority 3: UTR only match
     */
    private Optional<PaymentVerificationResponse> verifyWithBoth(PaymentVerificationRequest request) {
        log.info("Matching with BOTH orderId and utr");

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

        // No unprocessed match found
        log.warn("No unprocessed match found for OrderId: {}, UTR: {}", request.getOrderId(), request.getUtr());
        return Optional.empty();
    }

    /**
     * CASE 2: Only orderId provided
     * Matches by orderId only
     */
    private Optional<PaymentVerificationResponse> verifyWithOrderIdOnly(PaymentVerificationRequest request) {
        log.info("Matching with ONLY orderId (no UTR provided)");

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

        log.warn("No unprocessed match found for OrderId: {}", request.getOrderId());
        return Optional.empty();
    }

    /**
     * CASE 3: Only utr provided
     * Matches by utr only
     */
    private Optional<PaymentVerificationResponse> verifyWithUtrOnly(PaymentVerificationRequest request) {
        log.info("Matching with ONLY utr (no orderId provided)");

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

        log.warn("No unprocessed match found for UTR: {}", request.getUtr());
        return Optional.empty();
    }

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
     * Build verification response with match flags
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
}