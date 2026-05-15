package com.Application.SocietyManagement.communication.email.controller;

import com.Application.SocietyManagement.communication.email.dto.TestEmailRequest;
import com.Application.SocietyManagement.communication.email.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/communication/email")
@RequiredArgsConstructor
@Tag(name = "Email", description = "Email notification management")
public class EmailController {

    private final EmailService emailService;

    @Operation(summary = "Send test email",
            description = "Sends a test email to verify email configuration. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email sent successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "500", description = "Failed to send email")
    })
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> sendTestEmail(
            @RequestBody @Valid TestEmailRequest request) {
        emailService.sendTestEmail(request.getTo(), request.getSubject(), request.getBody());
        return ResponseEntity.ok(Map.of("message", "Email sent successfully"));
    }
}
