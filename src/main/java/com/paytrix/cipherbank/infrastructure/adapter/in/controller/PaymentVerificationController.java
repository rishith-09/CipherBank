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
     * Priority matching logic:
     * 1. Check for orderId + utr match (both flags true)
     * 2. If not found, check for orderId only match (orderIdMatch true, utrMatch false)
     * 3. If not found, check for utr only match (orderIdMatch false, utrMatch true)
     * 4. If no match found, return 204 No Content
     *
     * Amount comparison: Always returns amount from database and sets amtMatch flag
     *
     * @param request Payment verification request
     * @return 200 OK with verification response if match found, 204 No Content if no match
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request) {

        log.debug("=== PAYMENT VERIFICATION REQUEST ===");
        log.debug("Gateway Transaction ID: {}", request.getGatewayTransactionId());
        log.debug("Order ID: {}", request.getOrderId());
        log.debug("UTR: {}", request.getUtr());
        log.debug("Amount: {}", request.getAmt());

        try {
            Optional<PaymentVerificationResponse> response = paymentVerificationUseCase.verifyPayment(request);

            if (response.isPresent()) {
                PaymentVerificationResponse verificationResponse = response.get();

                log.debug("=== VERIFICATION RESULT ===");
                log.debug("Match found!");
                log.debug("Order ID Match: {}", verificationResponse.getOrderIdMatch());
                log.debug("UTR Match: {}", verificationResponse.getUtrMatch());
                log.debug("Amount Match: {}", verificationResponse.getAmtMatch());
                log.debug("DB Amount: {}", verificationResponse.getAmt());
                log.debug("===========================");

                return ResponseEntity.ok(verificationResponse);
            } else {
                log.warn("=== VERIFICATION RESULT ===");
                log.warn("No match found for OrderId: {}, UTR: {}", request.getOrderId(), request.getUtr());
                log.warn("Returning 204 No Content");
                log.warn("===========================");

                return ResponseEntity.noContent().build();
            }

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