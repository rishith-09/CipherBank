package com.paytrix.cipherbank.application.port.in;

import com.paytrix.cipherbank.domain.model.PaymentVerificationRequest;
import com.paytrix.cipherbank.domain.model.PaymentVerificationResponse;

import java.util.Optional;

/**
 * Use case for verifying payments from gateway
 * Checks if payment details match existing bank statements
 */
public interface PaymentVerificationUseCase {

    /**
     * Verify payment details against bank statements
     *
     * Priority matching logic:
     * 1. Check for orderId + utr match (exact match)
     * 2. If not found, check for orderId only match
     * 3. If not found, check for utr only match
     * 4. If no match found, return empty
     *
     * @param request Payment verification request from gateway
     * @return Optional containing verification response if match found, empty if no match
     */
    Optional<PaymentVerificationResponse> verifyPayment(PaymentVerificationRequest request);
}