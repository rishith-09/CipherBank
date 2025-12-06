package com.paytrix.cipherbank.infrastructure.exception;

/**
 * Exception thrown when duplicate bank statement records are detected during upload.
 * This is expected behavior and should be handled gracefully.
 */
public class DuplicateStatementException extends RuntimeException {

    private final int totalParsed;
    private final int inserted;
    private final int duplicates;

    public DuplicateStatementException(String message, int totalParsed, int inserted, int duplicates) {
        super(message);
        this.totalParsed = totalParsed;
        this.inserted = inserted;
        this.duplicates = duplicates;
    }

    public int getTotalParsed() {
        return totalParsed;
    }

    public int getInserted() {
        return inserted;
    }

    public int getDuplicates() {
        return duplicates;
    }
}