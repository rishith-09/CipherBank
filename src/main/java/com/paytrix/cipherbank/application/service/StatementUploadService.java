package com.paytrix.cipherbank.application.service;

import com.paytrix.cipherbank.application.port.in.StatementUploadUseCase;
import com.paytrix.cipherbank.application.port.out.business.BankProfileRepositoryPort;
import com.paytrix.cipherbank.application.port.out.business.BankStatementRepositoryPort;
import com.paytrix.cipherbank.application.port.out.business.BankStatementUploadRepositoryPort;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business.ApprovalStatus;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business.BankStatement;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.business.BankStatementUpload;
import com.paytrix.cipherbank.infrastructure.config.parser.ParserConfigLoader;
import com.paytrix.cipherbank.infrastructure.parser.ParserEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class StatementUploadService implements StatementUploadUseCase {

    private static final Logger log = LoggerFactory.getLogger(StatementUploadService.class);

    private final BankProfileRepositoryPort bankProfileRepo;
    private final BankStatementUploadRepositoryPort uploadRepo;
    private final BankStatementRepositoryPort stmtRepo;
    private final ParserConfigLoader configLoader;

    public StatementUploadService(BankProfileRepositoryPort bankProfileRepo,
                                  BankStatementUploadRepositoryPort uploadRepo,
                                  BankStatementRepositoryPort stmtRepo,
                                  ParserConfigLoader configLoader) {

        this.bankProfileRepo = bankProfileRepo;
        this.uploadRepo = uploadRepo;
        this.stmtRepo = stmtRepo;
        this.configLoader = configLoader;
    }

    @Override
    @Transactional
    public UploadResult upload(UploadCommand cmd) {
        log.info("Starting upload process for parserKey: {}, username: {}", cmd.parserKey(), cmd.username());

        var bank = bankProfileRepo.findByParserKey(cmd.parserKey())
                .orElseThrow(() -> new IllegalArgumentException("Unknown parserKey: " + cmd.parserKey()));

        // create upload row
        var upload = new BankStatementUpload();
        upload.setUsername(cmd.username());
        upload.setUploadTime(Instant.now());
        upload.setBank(bank);
        upload = uploadRepo.save(upload);

        log.info("Created upload record with ID: {}", upload.getId());

        int inserted = 0, deduped = 0, parsed = 0;
        String accountNo = cmd.accountNoOverride(); // optional override

        try {
            var bankCfg = configLoader.getBankConfig(cmd.parserKey());
            var rows = ParserEngine.parse(cmd.inputStream(), cmd.originalFilename(), cmd.contentType(), bankCfg, accountNo);
            parsed = rows.size();

            log.info("Parsed {} rows from file: {}", parsed, cmd.originalFilename());

            for (var r : rows) {
                // DEDUPLICATION CHECK - Check if record already exists with same UTR, Order ID, and Account No
                Long accountNumber = r.getAccountNo() != null ? Long.parseLong(r.getAccountNo()) : null;

                boolean isDuplicate = stmtRepo.existsByUtrAndOrderIdAndAccountNo(
                        r.getUtr(),
                        r.getOrderId(),
                        accountNumber
                );

                if (isDuplicate) {
                    // Skip this record - it's a duplicate
                    deduped++;
                    log.debug("Skipped duplicate: UTR={}, OrderID={}, AccountNo={}",
                            r.getUtr(), r.getOrderId(), accountNumber);
                    continue;
                }

                // Not a duplicate - proceed with insertion
                var s = new BankStatement();
                s.setUpload(upload);
                s.setTransactionDateTime(r.getTransactionDateTime());
                s.setAmount(r.getAmount());
                s.setBalance(r.getBalance());
                s.setReference(r.getReference());
                s.setOrderId(r.getOrderId());
                s.setUtr(r.getUtr());
                s.setPayIn(r.isPayIn());
                s.setApprovalStatus(ApprovalStatus.PENDING);
                s.setProcessed(false);
                s.setType(r.getOrderId()); // optional: mimic your fallback
                s.setUploadTimestamp(Instant.now());
                s.setAccountNo(accountNumber);

                var saved = stmtRepo.save(s);
                if (saved != null) {
                    inserted++;
                    log.debug("Inserted statement: UTR={}, OrderID={}, AccountNo={}",
                            r.getUtr(), r.getOrderId(), accountNumber);
                } else {
                    // Save failed (might be caught by DB constraint as backup)
                    deduped++;
                    log.warn("Failed to save statement (DB constraint): UTR={}, OrderID={}, AccountNo={}",
                            r.getUtr(), r.getOrderId(), accountNumber);
                }
            }

            log.info("Upload processing complete - Parsed: {}, Inserted: {}, Deduped: {}",
                    parsed, inserted, deduped);

        } catch (Exception ex) {
            log.error("Failed to process file: {}", cmd.originalFilename(), ex);
            throw new RuntimeException("Failed to process file: " + cmd.originalFilename(), ex);
        }

        return new UploadResult(upload.getId(), cmd.parserKey(), accountNo, parsed, inserted, deduped, upload.getUploadTime());
    }
}