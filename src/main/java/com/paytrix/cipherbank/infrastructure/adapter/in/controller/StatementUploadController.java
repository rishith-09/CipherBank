package com.paytrix.cipherbank.infrastructure.adapter.in.controller;

import com.paytrix.cipherbank.application.port.in.StatementUploadUseCase;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import java.io.IOException;

@RestController
@RequestMapping("/api/statements")
@Validated
public class StatementUploadController {

    private final StatementUploadUseCase useCase;

    public StatementUploadController(StatementUploadUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','USER','ROLE_ADMIN','ROLE_USER')")
    public ResponseEntity<StatementUploadUseCase.UploadResult> upload(
            @RequestParam("parserKey") @NotBlank String parserKey,
            @RequestParam("username")  @NotBlank String username,
            @RequestParam(value = "accountNo", required = false) String accountNo,
            @RequestPart("file") MultipartFile file
    ) throws IOException {

        var cmd = new StatementUploadUseCase.UploadCommand(
                parserKey,
                username,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getInputStream(),
                accountNo
        );
        var result = useCase.upload(cmd);
        return ResponseEntity.ok(result);
    }
}
