package com.Application.SocietyManagement.subscription.service;

import com.Application.SocietyManagement.society.enums.PaymentStatus;
import com.Application.SocietyManagement.subscription.entity.Subscription;
import com.Application.SocietyManagement.subscription.repository.SubscriptionRepository;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookService")
class WebhookServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private WebhookService webhookService;

    private static final String WEBHOOK_SECRET = "whsec_supersecret123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(webhookService, "webhookSecret", WEBHOOK_SECRET);
    }

    private String createPayload(String event, String orderId, String paymentId, String errorDesc) throws Exception {
        JSONObject paymentEntity = new JSONObject();
        paymentEntity.put("id", paymentId);
        paymentEntity.put("order_id", orderId);
        if (errorDesc != null) {
            paymentEntity.put("error_description", errorDesc);
        }

        JSONObject payment = new JSONObject();
        payment.put("entity", paymentEntity);

        JSONObject payload = new JSONObject();
        payload.put("payment", payment);

        JSONObject body = new JSONObject();
        body.put("event", event);
        body.put("payload", payload);

        return body.toString();
    }

    @Test
    @DisplayName("invalid webhook signature returns false")
    void handleWebhook_invalidSignature_returnsFalse() {
        String payload = "{\"event\":\"payment.captured\"}";
        boolean result = webhookService.handleWebhook(payload, "invalid_sig");

        assertThat(result).isFalse();
        verifyNoInteractions(subscriptionRepository);
        verifyNoInteractions(subscriptionService);
    }

    @Test
    @DisplayName("payment.captured for known order activates subscription")
    void handleWebhook_paymentCaptured_success() throws Exception {
        String payload = createPayload("payment.captured", "order_cap_123", "pay_xyz", null);
        String signature = Utils.getHash(payload, WEBHOOK_SECRET);

        Subscription subscription = Subscription.builder()
                .razorpayOrderId("order_cap_123")
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(subscriptionRepository.findByRazorpayOrderId("order_cap_123"))
                .thenReturn(Optional.of(subscription));

        boolean handled = webhookService.handleWebhook(payload, signature);

        assertThat(handled).isTrue();
        verify(subscriptionService).activateSubscription(subscription, "pay_xyz", "verified-via-webhook");
    }

    @Test
    @DisplayName("payment.captured for unknown order skips activation")
    void handleWebhook_paymentCaptured_unknownOrder() throws Exception {
        String payload = createPayload("payment.captured", "order_unknown", "pay_xyz", null);
        String signature = Utils.getHash(payload, WEBHOOK_SECRET);

        when(subscriptionRepository.findByRazorpayOrderId("order_unknown")).thenReturn(Optional.empty());

        boolean handled = webhookService.handleWebhook(payload, signature);

        assertThat(handled).isTrue();
        verify(subscriptionService, never()).activateSubscription(any(), any(), any());
    }

    @Test
    @DisplayName("payment.failed marks subscription as FAILED")
    void handleWebhook_paymentFailed_marksFailed() throws Exception {
        String payload = createPayload("payment.failed", "order_fail_123", "pay_failed", "Insufficient balance");
        String signature = Utils.getHash(payload, WEBHOOK_SECRET);

        Subscription subscription = Subscription.builder()
                .razorpayOrderId("order_fail_123")
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(subscriptionRepository.findByRazorpayOrderId("order_fail_123"))
                .thenReturn(Optional.of(subscription));

        boolean handled = webhookService.handleWebhook(payload, signature);

        assertThat(handled).isTrue();
        assertThat(subscription.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(subscription.getFailureReason()).isEqualTo("Insufficient balance");
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    @DisplayName("payment.failed does not overwrite already SUCCESS subscription")
    void handleWebhook_paymentFailed_alreadySuccess_doesNotOverwrite() throws Exception {
        String payload = createPayload("payment.failed", "order_already_success", "pay_failed", "Late failure");
        String signature = Utils.getHash(payload, WEBHOOK_SECRET);

        Subscription subscription = Subscription.builder()
                .razorpayOrderId("order_already_success")
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        when(subscriptionRepository.findByRazorpayOrderId("order_already_success"))
                .thenReturn(Optional.of(subscription));

        boolean handled = webhookService.handleWebhook(payload, signature);

        assertThat(handled).isTrue();
        assertThat(subscription.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("unhandled event returns true without error")
    void handleWebhook_unhandledEvent_returnsTrue() throws Exception {
        JSONObject body = new JSONObject();
        body.put("event", "refund.processed");
        String payload = body.toString();
        String signature = Utils.getHash(payload, WEBHOOK_SECRET);

        boolean handled = webhookService.handleWebhook(payload, signature);

        assertThat(handled).isTrue();
    }
}
