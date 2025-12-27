package com.paytrix.cipherbank.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for payment verification
 * Contains match flags for orderId, utr, and amount comparison
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVerificationResponse {

    private Long gatewayTransactionId;

    private String orderId;

    private String utr;

    private Long accountNo;

    /**
     * Amount from database (if record found)
     */
    private BigDecimal amt;

    /**
     * True if orderId matches a record in database
     */
    private Boolean orderIdMatch;

    /**
     * True if UTR matches a record in database
     */
    private Boolean utrMatch;

    /**
     * True if amount matches the database record amount
     */
    private Boolean amtMatch;
}