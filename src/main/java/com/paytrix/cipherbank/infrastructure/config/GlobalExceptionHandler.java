package com.paytrix.cipherbank.infrastructure.config;

import com.paytrix.cipherbank.infrastructure.exception.*;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for all controllers
 * Ensures proper HTTP status codes and error messages are returned
 * instead of generic 403 Forbidden errors
 *
 * FEATURES:
 * - Maps all exceptions to proper HTTP status codes
 * - Returns consistent error response format
 * - Includes detailed error info in development mode
 * - Comprehensive logging at appropriate levels
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    /**
     * Check if running in development mode
     */
    private boolean isDevelopmentMode() {
        return "dev".equals(activeProfile) ||
                "local".equals(activeProfile) ||
                "development".equals(activeProfile);
    }

    /**
     * Standard error response structure
     */
    private Map<String, Object> buildErrorResponse(
            String error,
            String message,
            HttpStatus status,
            WebRequest request
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        response.put("path", request.getDescription(false).replace("uri=", ""));
        return response;
    }

    /**
     * Add development details to error response
     */
    private void addDevelopmentDetails(Map<String, Object> response, Exception ex) {
        if (isDevelopmentMode()) {
            response.put("exceptionType", ex.getClass().getSimpleName());
            response.put("detailedMessage", ex.getMessage());

            // Add first 5 stack trace elements for context
            String stackTrace = Arrays.stream(ex.getStackTrace())
                    .limit(5)
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n  at "));
            response.put("stackTrace", stackTrace);

            // Add cause if present
            if (ex.getCause() != null) {
                response.put("cause", ex.getCause().getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Authentication & Authorization Errors (401/403)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Handle authentication failures (invalid credentials)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex,
            WebRequest request
    ) {
        log.warn("Authentication failed: {}", ex.getMessage());

        Map<String, Object> response = buildErrorResponse(
                "Unauthorized",
                "Invalid username or password",
                HttpStatus.UNAUTHORIZED,
                request
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle general authentication errors
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex,
            WebRequest request
    ) {
        log.warn("Authentication error: {}", ex.getMessage());

        Map<String, Object> response = buildErrorResponse(
                "Unauthorized",
                "Authentication failed: " + ex.getMessage(),
                HttpStatus.UNAUTHORIZED,
                request
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle access denied (user authenticated but lacks permission)
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex,
            WebRequest request
    ) {
        log.warn("Access denied: {}", ex.getMessage());

        Map<String, Object> response = buildErrorResponse(
                "Forbidden",
                "You don't have permission to access this resource",
                HttpStatus.FORBIDDEN,
                request
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Validation Errors (400)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        log.warn("Validation error: {}", ex.getMessage());

        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        Map<String, Object> response = buildErrorResponse(
                "Bad Request",
                "Validation failed: " + errors,
                HttpStatus.BAD_REQUEST,
                request
        );

        addDevelopmentDetails(response, ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle constraint violations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request
    ) {
        log.warn("Constraint violation: {}", ex.getMessage());

        String errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        Map<String, Object> response = buildErrorResponse(
                "Bad Request",
                "Validation failed: " + errors,
                HttpStatus.BAD_REQUEST,
                request
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParams(
            MissingServletRequestParameterException ex,
            WebRequest request
    ) {
        log.warn("Missing parameter: {}", ex.getMessage());

        Map<String, Object> response = buildErrorResponse(
                "Bad Request",
                "Required parameter '" + ex.getParameterName() + "' is missing",
                HttpStatus.BAD_REQUEST,
                request
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle illegal arguments
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request
    ) {
        log.warn("Illegal argument: {}", ex.getMessage());

        Map<String, Object> response = buildErrorResponse(
                "Bad Request",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Resource Errors (404/409)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Handle resource not found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex,
            WebRequest request
    ) {
        log.warn("Resource not found: {}", ex.getMessage());

        Map<String, Object> response = buildErrorResponse(
                "Not Found",
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                request
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle endpoint not found (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFound(
            NoHandlerFoundException ex,
            WebRequest request
    ) {
        log.warn("No handler found: {}", ex.getMessage());

        Map<String, Object> response = buildErrorResponse(
                "Not Found",
                "Endpoint not found: " + ex.getRequestURL(),
                HttpStatus.NOT_FOUND,
                request
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle duplicate resource conflicts
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResource(
            DuplicateResourceException ex,
            WebRequest request
    ) {
        log.warn("Duplicate resource: {}", ex.getMessage());

        Map<String, Object> response = buildErrorResponse(
                "Conflict",
                ex.getMessage(),
                HttpStatus.CONFLICT,
                request
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle multiple records found (data inconsistency)
     * NEW: Added for payment verification
     */
    @ExceptionHandler(MultipleRecordsFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMultipleRecordsFound(
            MultipleRecordsFoundException ex,
            WebRequest request
    ) {
        log.error("Multiple records found - Data inconsistency: {}", ex.getMessage());

        Map<String, Object> response = buildErrorResponse(
                "Conflict",
                ex.getMessage(),
                HttpStatus.CONFLICT,
                request
        );

        // Add additional context for debugging
        response.put("recordCount", ex.getRecordCount());
        response.put("matchType", ex.getMatchType());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // File Upload Errors (413/415)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Handle file size exceeded
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex,
            WebRequest request
    ) {
        log.warn("File size exceeded: {}", ex.getMessage());

        Map<String, Object> response = buildErrorResponse(
                "Payload Too Large",
                "File size exceeds maximum limit (10MB)",
                HttpStatus.PAYLOAD_TOO_LARGE,
                request
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    /**
     * Handle invalid file type
     */
    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFileType(
            InvalidFileTypeException ex,
            WebRequest request
    ) {
        log.warn("Invalid file type: {}", ex.getMessage());

        Map<String, Object> response = buildErrorResponse(
                "Unsupported Media Type",
                ex.getMessage(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                request
        );

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Business Logic Errors (422)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Handle business logic errors
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(
            BusinessException ex,
            WebRequest request
    ) {
        log.warn("Business exception: {}", ex.getMessage());

        Map<String, Object> response = buildErrorResponse(
                "Unprocessable Entity",
                ex.getMessage(),
                HttpStatus.UNPROCESSABLE_ENTITY,
                request
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    /**
     * Handle file processing errors
     */
    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleFileProcessing(
            FileProcessingException ex,
            WebRequest request
    ) {
        log.error("File processing error: {}", ex.getMessage(), ex);

        Map<String, Object> response = buildErrorResponse(
                "Unprocessable Entity",
                ex.getMessage(),
                HttpStatus.UNPROCESSABLE_ENTITY,
                request
        );

        addDevelopmentDetails(response, ex);

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Database Errors (500/503)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Handle database errors
     */
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(
            org.springframework.dao.DataAccessException ex,
            WebRequest request
    ) {
        log.error("Database error: {}", ex.getMessage(), ex);

        Map<String, Object> response = buildErrorResponse(
                "Service Unavailable",
                "Database error occurred. Please try again later.",
                HttpStatus.SERVICE_UNAVAILABLE,
                request
        );

        addDevelopmentDetails(response, ex);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Generic Errors (500)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Handle all other uncaught exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex,
            WebRequest request
    ) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        Map<String, Object> response = buildErrorResponse(
                "Internal Server Error",
                "An unexpected error occurred. Please contact support if the problem persists.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );

        addDevelopmentDetails(response, ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}