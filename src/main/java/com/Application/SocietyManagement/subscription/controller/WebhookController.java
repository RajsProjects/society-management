package com.Application.SocietyManagement.subscription.controller;

import com.Application.SocietyManagement.subscription.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        boolean valid = webhookService.handleWebhook(payload, signature);
        if (!valid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }
        return ResponseEntity.ok("OK");
    }
}