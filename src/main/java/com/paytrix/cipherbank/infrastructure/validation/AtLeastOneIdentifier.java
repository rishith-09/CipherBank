package com.paytrix.cipherbank.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation to ensure at least one of orderId or utr is present
 * Used on PaymentVerificationRequest class level
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AtLeastOneIdentifierValidator.class)
@Documented
public @interface AtLeastOneIdentifier {

    String message() default "At least one of orderId or utr must be provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}