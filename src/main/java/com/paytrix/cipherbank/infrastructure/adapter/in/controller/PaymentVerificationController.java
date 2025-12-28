package com.paytrix.cipherbank.infrastructure.adapter.in.controller;

import com.paytrix.cipherbank.application.port.in.PaymentVerificationUseCase;
import com.paytrix.cipherbank.domain.model.PaymentVerificationRequest;
import com.paytrix.cipherbank.domain.model.PaymentVerificationResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller for payment verification from gateway
 *
 * SECURITY NOTE:
 * This endpoint is protected by SecurityConfig which requires authentication.
 * Consider adding IP whitelisting or API key authentication for gateway-to-gateway communication.
 */
@RestController
@RequestMapping("/api/payments")
@Slf4j
public class PaymentVerificationController {

    private final PaymentVerificationUseCase paymentVerificationUseCase;

    public PaymentVerificationController(PaymentVerificationUseCase paymentVerificationUseCase) {
        this.paymentVerificationUseCase = paymentVerificationUseCase;
    }

    /**
     * Verify payment details against bank statements
     *
     * NEW: Supports optional accountNo filtering
     * - If accountNo provided: Matches only within that specific account
     * - If accountNo NOT provided: Searches across all accounts (original behavior)
     *
     * Priority matching logic (unchanged):
     * 1. Check for orderId + utr match (both flags true)
     * 2. If not found, check for orderId only match (orderIdMatch true, utrMatch false)
     * 3. If not found, check for utr only match (orderIdMatch false, utrMatch true)
     * 4. If no match found, return "no match" response with all flags false
     *
     * NEW: Always returns 200 OK
     * - Match found: Returns response with match flags true and DB values
     * - No match found: Returns response with all flags false, amt/accountNo null, but preserves request values
     *
     * Amount comparison: Always returns amount from database and sets amtMatch flag
     *
     * @param request Payment verification request (with optional accountNo)
     * @return 200 OK with verification response (always includes response body)
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request) {

        log.debug("=== PAYMENT VERIFICATION REQUEST ===");
        log.debug("Gateway Transaction ID: {}", request.getGatewayTransactionId());
        log.debug("Order ID: {}", request.getOrderId());
        log.debug("UTR: {}", request.getUtr());
        log.debug("Amount: {}", request.getAmt());
        log.debug("Account No: {}", request.getAccountNo() != null ? request.getAccountNo() : "NOT PROVIDED (search all)");

        try {
            Optional<PaymentVerificationResponse> response = paymentVerificationUseCase.verifyPayment(request);

            // Service always returns a value now (never empty)
            PaymentVerificationResponse verificationResponse = response.orElseThrow(
                    () -> new IllegalStateException("Service should never return empty Optional"));

            log.debug("=== VERIFICATION RESULT ===");
            log.debug("Order ID Match: {}", verificationResponse.getOrderIdMatch());
            log.debug("UTR Match: {}", verificationResponse.getUtrMatch());
            log.debug("Amount Match: {}", verificationResponse.getAmtMatch());
            log.debug("DB Amount: {}", verificationResponse.getAmt());
            log.debug("DB Account No: {}", verificationResponse.getAccountNo());

            if (verificationResponse.getOrderIdMatch() || verificationResponse.getUtrMatch()) {
                log.debug("Match found!");
            } else {
                log.debug("No match found - returning response with all flags false");
            }
            log.debug("===========================");

            // Always return 200 OK with response body
            return ResponseEntity.ok(verificationResponse);

        } catch (Exception e) {
            log.error("Error during payment verification: {}", e.getMessage(), e);
            throw e; // GlobalExceptionHandler will handle it
        }
    }

    /**
     * Health check endpoint for payment verification service
     */
    @GetMapping("/verify/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Payment verification service is running");
    }
}