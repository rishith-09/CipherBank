package com.paytrix.cipherbank.infrastructure.exception;

/**
 * Exception thrown when payment verification finds multiple matching records
 * This indicates data inconsistency - there should only be one match
 * Results in HTTP 409 Conflict
 */
public class MultipleRecordsFoundException extends RuntimeException {

    private final int recordCount;
    private final String matchType;

    public MultipleRecordsFoundException(int recordCount, String matchType, String criteria) {
        super(String.format(
                "Data inconsistency: Found %d records matching %s for %s. Expected exactly 1 record.",
                recordCount, matchType, criteria
        ));
        this.recordCount = recordCount;
        this.matchType = matchType;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public String getMatchType() {
        return matchType;
    }
}