package com.Application.SocietyManagement.subscription.controller;

import com.Application.SocietyManagement.subscription.dto.CreateOrderRequest;
import com.Application.SocietyManagement.subscription.dto.OrderResponse;
import com.Application.SocietyManagement.subscription.dto.VerifyPaymentRequest;
import com.Application.SocietyManagement.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/create-order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(subscriptionService.createOrder(request.getPlan()));
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<Void> verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {
        subscriptionService.verifyPayment(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-plan")
    public ResponseEntity<OrderResponse> changePlan(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(subscriptionService.changePlan(request.getPlan()));
    }
}