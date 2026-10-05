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

import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.PlatformRole;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/communication/email")
@RequiredArgsConstructor
@Tag(name = "Email", description = "Email notification management")
public class EmailController {

    private final EmailService emailService;

    @Operation(summary = "Send test email",
            description = "Sends a test email to verify email configuration. Restricted to admin's own email unless platform admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email sent successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "500", description = "Failed to send email")
    })
    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, String>> sendTestEmail(
            @RequestBody @Valid TestEmailRequest request,
            @AuthenticationPrincipal User currentUser) {
        boolean isPlatformAdmin = currentUser != null && currentUser.getPlatformRole() == PlatformRole.PLATFORM_ADMIN;
        String recipient = (isPlatformAdmin && request.getTo() != null && !request.getTo().isBlank())
                ? request.getTo()
                : (currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail() : request.getTo());

        emailService.sendTestEmail(recipient, request.getSubject(), request.getBody());
        return ResponseEntity.ok(Map.of(
                "message", "Email sent successfully",
                "recipient", recipient
        ));
    }
}
