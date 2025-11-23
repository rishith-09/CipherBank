package com.paytrix.cipherbank.infrastructure.config.parser;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ParserConfig {
    private Map<String, BankConfig> banks;

    @Data
    public static class BankConfig {
        private String parserKey;
        private String timezone;        // e.g., Asia/Kolkata
        private FileTypeConfig csv;
        private FileTypeConfig xlsx;
        private FileTypeConfig xls;
        private PdfConfig pdf;
    }

    @Data
    public static class FileTypeConfig {
        private String sheetIndex;      // for excel
        private Headers headers;
        private Account account;
        private DateParse dateParse;
        private Reference reference;
        private PayInRule payInRule;
        private Numeric numeric;
        private RowStop rowStop;
        private Output output;
        private Csv csv;                // csv-specific
    }

    @Data
    public static class Csv {
        private String delimiter;       // ","
        private String charset;         // "UTF-8"
        private Integer skipRows;       // optional
    }

    @Data
    public static class PdfConfig {
        private String timezone;
        private PdfTable pdfTable;
        private DateParse dateParse;
        private Reference reference;
        private PayInRule payInRule;
        private Numeric numeric;
        private Output output;
    }

    @Data
    public static class PdfTable {
        private String startAfterRegex;
        private String stopBeforeRegex;
        private String linePattern;     // named groups: date, ref, credit, debit, amount, balance
        private String accountRegex;
    }

    @Data
    public static class Headers {
        private String mode;            // "fixed" | "search"
        private Fixed fixed;
        private Search search;
    }

    @Data
    public static class Fixed {
        private Integer rowStart;
        private Columns columns;
    }

    @Data
    public static class Search {
        private Range scanRange;            // where to look for header (start row .. end row)
        private Map<String, List<String>> expect;   // header synonyms for keys
        private Integer rowStartOffset;     // typically 1
        // NEW: multi-row header options
        private Integer multiRowCount;      // number of header rows to merge (default 1)
        private Range fixedHeaderRows;      // if set, use these rows (inclusive) as header window
        private Boolean useOneBasedRowIndex; // default true (matches Excel UI)
        private String mergeSeparator;      // default " "
    }

    @Data
    public static class Range {
        private Integer from;   // inclusive
        private Integer to;     // inclusive
    }

    @Data
    public static class Columns {
        private Integer date;
        private Integer time;
        private Integer reference;
        private Integer credit;
        private Integer debit;
        private Integer amount;
        private Integer balance;
    }

    @Data
    public static class Account {
        private Boolean required;           // if true, account number is mandatory
        private String source;              // "excelCell" | "csvCell" | "textRegex" | "request"
        private ExcelCell excelCell;        // for Excel files (xlsx/xls)
        private CsvCell csvCell;            // for CSV files (not commonly used)
        private String textRegex;           // regex to extract from text
        private String cleanupRegex;        // regex to cleanup extracted value (e.g., "\\D" to keep only digits)
        private Boolean useOneBasedIndex;   // default true for Excel row/col indices
        private Integer mergedColumnEnd;    // if account spans merged cells, specify end column
    }

    @Data
    public static class ExcelCell {
        private Integer row;     // row number (1-based by default)
        private Integer col;     // column number (1-based by default)
    }

    @Data
    public static class CsvCell {
        private Integer row;     // row number
        private Integer col;     // column number
    }

    @Data
    public static class DateParse {
        private String input;           // "excelSerial" | "string"
        private String format;          // "dd/MM/yyyy"
        private Boolean withTimeInSameField;
        private String timeFormat;      // "HH:mm:ss"
    }

    @Data
    public static class Reference {
        private String splitter;        // "/"
        private PartsCount partsCount;
        private IndexDef orderId;
        private IndexDef utr;
        private Fallback utrFallback;
        private SkipIf skipIf;
    }

    @Data
    public static class PartsCount { private String mode; private List<Integer> values; } // "exact"|"oneOf"
    @Data
    public static class IndexDef { private Integer index; private Boolean cleanDigitsOnly; private Boolean trimNbsp; }
    @Data
    public static class Fallback { private String when; private String regex; }
    @Data
    public static class SkipIf { private Boolean emptyUtr; }

    @Data
    public static class PayInRule {
        private String type;            // amountPositive | creditColumn | orderIdNoSpace | utrNoSpace
        private List<String> narrationContainsAny;
    }

    @Data
    public static class Numeric {
        private String decimalSeparator;    // "."
        private String thousandsSeparator;  // ","
    }

    @Data
    public static class RowStop {
        private String mode;            // "blankRows" | "until" | "none"
        private String untilRegex;
    }

    @Data
    public static class Output {
        private Boolean includeFinalBalance;
        private String computeTypeFrom; // e.g., "orderId"
    }
}