package com.Application.SocietyManagement.subscription.service;

import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.society.entity.Society;
import com.Application.SocietyManagement.society.enums.PaymentStatus;
import com.Application.SocietyManagement.society.enums.SubscriptionPlan;
import com.Application.SocietyManagement.society.enums.SubscriptionStatus;
import com.Application.SocietyManagement.society.repository.SocietyRepository;
import com.Application.SocietyManagement.subscription.config.PlanPricingConfig;
import com.Application.SocietyManagement.subscription.dto.OrderResponse;
import com.Application.SocietyManagement.subscription.dto.VerifyPaymentRequest;
import com.Application.SocietyManagement.subscription.entity.Subscription;
import com.Application.SocietyManagement.subscription.repository.SubscriptionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SocietyRepository societyRepository;
    private final RazorpayClient razorpayClient;
    private final PlanPricingConfig planPricingConfig;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    public OrderResponse createOrder(SubscriptionPlan plan) {
        String societyId = TenantContext.getSocietyId();
        Society society = societyRepository.findById(societyId)
                .orElseThrow(() -> new RuntimeException("Society not found"));

        long amount = planPricingConfig.getAmountFor(plan);

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "sub_" + UUID.randomUUID());

            Order order = razorpayClient.orders.create(orderRequest);

            Subscription subscription = Subscription.builder()
                    .societyId(societyId)
                    .plan(plan)
                    .status(SubscriptionStatus.ACTIVE) // becomes effective on payment success
                    .amount(amount)
                    .currency("INR")
                    .razorpayOrderId(order.get("id"))
                    .paymentStatus(PaymentStatus.PENDING)
                    .build();

            subscriptionRepository.save(subscription);

            return OrderResponse.builder()
                    .subscriptionId(subscription.getId())
                    .razorpayOrderId(order.get("id"))
                    .amount(amount)
                    .currency("INR")
                    .keyId(keyId)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }

    public void verifyPayment(VerifyPaymentRequest request) {
        Subscription subscription = subscriptionRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Subscription order not found"));

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", request.getRazorpayOrderId());
        options.put("razorpay_payment_id", request.getRazorpayPaymentId());
        options.put("razorpay_signature", request.getRazorpaySignature());

        boolean isValid;
        try {
            isValid = Utils.verifyPaymentSignature(options, webhookSecret);
        } catch (Exception e) {
            isValid = false;
        }

        if (!isValid) {
            subscription.setPaymentStatus(PaymentStatus.FAILED);
            subscription.setFailureReason("Signature verification failed");
            subscriptionRepository.save(subscription);
            throw new RuntimeException("Payment verification failed");
        }

        activateSubscription(subscription, request.getRazorpayPaymentId(), request.getRazorpaySignature());
    }

    // Called by both the client-side verify endpoint AND the webhook handler (idempotent)
    public void activateSubscription(Subscription subscription, String paymentId, String signature) {
        if (subscription.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return; // already processed — avoid double-activation from webhook + client racing
        }

        Instant now = Instant.now();
        Instant endDate = now.plus(30, ChronoUnit.DAYS);

        subscription.setPaymentStatus(PaymentStatus.SUCCESS);
        subscription.setRazorpayPaymentId(paymentId);
        subscription.setRazorpaySignature(signature);
        subscription.setPaidAt(now);
        subscription.setStartDate(now);
        subscription.setEndDate(endDate);
        subscriptionRepository.save(subscription);

        Society society = societyRepository.findById(subscription.getSocietyId())
                .orElseThrow(() -> new RuntimeException("Society not found"));
        society.setPlan(subscription.getPlan());
        society.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        society.setSubscriptionEndsAt(endDate);
        societyRepository.save(society);
    }

    public OrderResponse changePlan(SubscriptionPlan newPlan) {
        String societyId = TenantContext.getSocietyId();
        Society society = societyRepository.findById(societyId)
                .orElseThrow(() -> new RuntimeException("Society not found"));

        if (society.getPlan() == newPlan) {
            throw new IllegalArgumentException("Society is already on the " + newPlan + " plan");
        }
        if (newPlan == SubscriptionPlan.TRIAL) {
            throw new IllegalArgumentException("Cannot switch to TRIAL plan");
        }
        if (society.getSubscriptionStatus() == SubscriptionStatus.CANCELLED
                || society.getSubscriptionStatus() == SubscriptionStatus.TRIAL) {
            throw new IllegalArgumentException("Cannot change plan from current subscription status: "
                    + society.getSubscriptionStatus());
        }

        // Reuse existing order creation — webhook/verify will call activateSubscription()
        // which already updates Society.plan, so no extra logic needed here
        return createOrder(newPlan);
    }
}