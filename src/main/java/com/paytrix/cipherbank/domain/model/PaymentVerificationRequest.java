package com.paytrix.cipherbank.domain.model;

import com.paytrix.cipherbank.infrastructure.validation.AtLeastOneIdentifier;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for payment verification from gateway
 *
 * RULES:
 * - gatewayTransactionId: MANDATORY
 * - amt: MANDATORY
 * - orderId: OPTIONAL
 * - utr: OPTIONAL
 * - At least ONE of orderId or utr must be provided
 * - accountNo: NOT in request (comes from database)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@AtLeastOneIdentifier(message = "At least one of orderId or utr must be provided")
public class PaymentVerificationRequest {

    @NotNull(message = "Gateway transaction ID is required")
    private Long gatewayTransactionId;

    // OPTIONAL - but at least one of orderId or utr required
    private String orderId;

    // OPTIONAL - but at least one of orderId or utr required
    private String utr;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amt;
}