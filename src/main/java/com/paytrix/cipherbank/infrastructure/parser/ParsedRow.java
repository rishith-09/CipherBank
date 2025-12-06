package com.paytrix.cipherbank.infrastructure.parser;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single transaction row parsed from a bank statement.
 * All data is stored exactly as it appears in the statement, with NO timezone conversion.
 */
@Data
@NoArgsConstructor
public class ParsedRow {

    /**
     * Transaction date/time exactly as it appears in the bank statement.
     * NO timezone conversion applied.
     * If time is not in statement, it defaults to 00:00:00.
     */
    private LocalDateTime transactionDateTime;

    /**
     * Transaction amount (debit or credit)
     */
    private BigDecimal amount;

    /**
     * Account balance after transaction
     */
    private BigDecimal balance;

    /**
     * Payment reference or description
     */
    private String reference;

    /**
     * Order ID extracted from reference (if present)
     */
    private String orderId;

    /**
     * UTR (Unique Transaction Reference) - typically 12 digits
     */
    private String utr;

    /**
     * true if credit (money in), false if debit (money out)
     */
    private boolean payIn;

    /**
     * Bank account number
     * Can be extracted from file or provided via API request
     */
    private String accountNo;

    /**
     * Transaction type (e.g., UPI, NEFT, RTGS, etc.)
     */
    private String type;
}