package com.paytrix.cipherbank.infrastructure.validation;

import com.paytrix.cipherbank.domain.model.PaymentVerificationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator to ensure PaymentVerificationRequest has at least one identifier
 * Either orderId or utr (or both) must be present
 */
public class AtLeastOneIdentifierValidator
        implements ConstraintValidator<AtLeastOneIdentifier, PaymentVerificationRequest> {

    @Override
    public boolean isValid(PaymentVerificationRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // null handled by @NotNull on class if needed
        }

        String orderId = request.getOrderId();
        String utr = request.getUtr();

        // At least one must be non-null and non-blank
        boolean hasOrderId = orderId != null && !orderId.trim().isEmpty();
        boolean hasUtr = utr != null && !utr.trim().isEmpty();

        return hasOrderId || hasUtr;
    }
}