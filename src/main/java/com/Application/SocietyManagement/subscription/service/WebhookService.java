package com.Application.SocietyManagement.subscription.service;

import com.Application.SocietyManagement.society.enums.PaymentStatus;
import com.Application.SocietyManagement.subscription.entity.Subscription;
import com.Application.SocietyManagement.subscription.repository.SubscriptionRepository;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    public boolean handleWebhook(String payload, String signature) {
        boolean isValid;
        try {
            isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        } catch (Exception e) {
            log.error("Razorpay webhook signature check threw", e);
            return false;
        }

        if (!isValid) {
            log.warn("Razorpay webhook signature verification failed");
            return false;
        }

        JSONObject body = new JSONObject(payload);
        String event = body.getString("event");
        log.info("Razorpay webhook received: {}", event);

        switch (event) {
            case "payment.captured" -> handlePaymentCaptured(body);
            case "payment.failed" -> handlePaymentFailed(body);
            default -> log.info("Unhandled Razorpay event type: {}", event);
        }

        return true;
    }

    private void handlePaymentCaptured(JSONObject body) {
        JSONObject payment = body.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
        String orderId = payment.getString("order_id");
        String paymentId = payment.getString("id");

        Subscription subscription = subscriptionRepository.findByRazorpayOrderId(orderId).orElse(null);
        if (subscription == null) {
            log.warn("payment.captured webhook for unknown order_id={}", orderId);
            return;
        }

        // Idempotent: activateSubscription no-ops if already SUCCESS, so it's safe
        // whether this webhook or the client's /verify-payment call lands first.
        subscriptionService.activateSubscription(subscription, paymentId, "verified-via-webhook");
    }

    private void handlePaymentFailed(JSONObject body) {
        JSONObject payment = body.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
        String orderId = payment.getString("order_id");
        String reason = payment.optString("error_description", "Payment failed");

        Subscription subscription = subscriptionRepository.findByRazorpayOrderId(orderId).orElse(null);
        if (subscription == null) {
            log.warn("payment.failed webhook for unknown order_id={}", orderId);
            return;
        }
        if (subscription.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return; // already succeeded via another path — don't overwrite
        }

        subscription.setPaymentStatus(PaymentStatus.FAILED);
        subscription.setFailureReason(reason);
        subscriptionRepository.save(subscription);
    }
}