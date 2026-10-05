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
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService")
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SocietyRepository societyRepository;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private OrderClient orderClient;

    @Mock
    private PlanPricingConfig planPricingConfig;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private static final String SOCIETY_ID = "society-sub-123";
    private static final String KEY_ID = "rzp_test_key123";
    private static final String KEY_SECRET = "rzp_test_secret456";

    private Society society;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionService, "keyId", KEY_ID);
        ReflectionTestUtils.setField(subscriptionService, "keySecret", KEY_SECRET);
        ReflectionTestUtils.setField(subscriptionService, "webhookSecret", "whsec_test789");

        razorpayClient.orders = orderClient;

        TenantContext.setSocietyId(SOCIETY_ID);

        society = Society.builder()
                .name("Sunrise Apartments")
                .plan(SubscriptionPlan.TRIAL)
                .subscriptionStatus(SubscriptionStatus.TRIAL)
                .build();
        society.setId(SOCIETY_ID);

        subscription = Subscription.builder()
                .societyId(SOCIETY_ID)
                .plan(SubscriptionPlan.GROWTH)
                .amount(299900L)
                .currency("INR")
                .razorpayOrderId("order_test_999")
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        subscription.setId("sub-record-1");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("throws NOT_FOUND when society does not exist")
        void createOrder_societyNotFound_throwsNotFound() {
            when(societyRepository.findById(SOCIETY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.createOrder(SubscriptionPlan.BASIC))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("successful order creates razorpay order and saves pending subscription")
        void createOrder_success() throws Exception {
            when(societyRepository.findById(SOCIETY_ID)).thenReturn(Optional.of(society));
            when(planPricingConfig.getAmountFor(SubscriptionPlan.GROWTH)).thenReturn(299900L);

            JSONObject orderJson = new JSONObject();
            orderJson.put("id", "order_test_999");
            Order mockOrder = new Order(orderJson);

            when(orderClient.create(any(JSONObject.class))).thenReturn(mockOrder);
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
                Subscription s = inv.getArgument(0);
                s.setId("sub-record-1");
                return s;
            });

            OrderResponse response = subscriptionService.createOrder(SubscriptionPlan.GROWTH);

            assertThat(response).isNotNull();
            assertThat(response.getRazorpayOrderId()).isEqualTo("order_test_999");
            assertThat(response.getAmount()).isEqualTo(299900L);
            assertThat(response.getKeyId()).isEqualTo(KEY_ID);
            verify(subscriptionRepository).save(any(Subscription.class));
        }

        @Test
        @DisplayName("razorpay error throws BAD_GATEWAY")
        void createOrder_razorpayFails_throwsBadGateway() throws Exception {
            when(societyRepository.findById(SOCIETY_ID)).thenReturn(Optional.of(society));
            when(planPricingConfig.getAmountFor(SubscriptionPlan.GROWTH)).thenReturn(299900L);
            when(orderClient.create(any(JSONObject.class))).thenThrow(new RazorpayException("Gateway timeout"));

            assertThatThrownBy(() -> subscriptionService.createOrder(SubscriptionPlan.GROWTH))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
        }
    }

    @Nested
    @DisplayName("verifyPayment")
    class VerifyPaymentTests {

        @Test
        @DisplayName("throws IllegalStateException when society context is missing")
        void verifyPayment_noSocietyContext_throwsIllegalStateException() {
            TenantContext.clear();
            VerifyPaymentRequest req = new VerifyPaymentRequest();
            req.setRazorpayOrderId("order_test_999");

            assertThatThrownBy(() -> subscriptionService.verifyPayment(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No society context");
        }

        @Test
        @DisplayName("throws NOT_FOUND when subscription order is not found")
        void verifyPayment_orderNotFound_throwsNotFound() {
            VerifyPaymentRequest req = new VerifyPaymentRequest();
            req.setRazorpayOrderId("order_nonexistent");

            when(subscriptionRepository.findByRazorpayOrderIdAndSocietyId("order_nonexistent", SOCIETY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.verifyPayment(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("invalid signature marks payment as FAILED and throws BAD_REQUEST")
        void verifyPayment_invalidSignature_marksFailedAndThrows() {
            VerifyPaymentRequest req = new VerifyPaymentRequest();
            req.setRazorpayOrderId("order_test_999");
            req.setRazorpayPaymentId("pay_123");
            req.setRazorpaySignature("invalid_signature");

            when(subscriptionRepository.findByRazorpayOrderIdAndSocietyId("order_test_999", SOCIETY_ID))
                    .thenReturn(Optional.of(subscription));

            assertThatThrownBy(() -> subscriptionService.verifyPayment(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

            assertThat(subscription.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(subscription.getFailureReason()).isEqualTo("Signature verification failed");
            verify(subscriptionRepository).save(subscription);
        }

        @Test
        @DisplayName("valid signature activates subscription and updates society")
        void verifyPayment_validSignature_activatesSubscription() throws Exception {
            String orderId = "order_test_999";
            String paymentId = "pay_valid_123";
            String validSignature = Utils.getHash(orderId + "|" + paymentId, KEY_SECRET);

            VerifyPaymentRequest req = new VerifyPaymentRequest();
            req.setRazorpayOrderId(orderId);
            req.setRazorpayPaymentId(paymentId);
            req.setRazorpaySignature(validSignature);

            when(subscriptionRepository.findByRazorpayOrderIdAndSocietyId(orderId, SOCIETY_ID))
                    .thenReturn(Optional.of(subscription));
            when(societyRepository.findById(SOCIETY_ID)).thenReturn(Optional.of(society));

            subscriptionService.verifyPayment(req);

            assertThat(subscription.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(subscription.getRazorpayPaymentId()).isEqualTo(paymentId);
            assertThat(subscription.getStartDate()).isNotNull();
            assertThat(subscription.getEndDate()).isNotNull();

            assertThat(society.getPlan()).isEqualTo(SubscriptionPlan.GROWTH);
            assertThat(society.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(society.getSubscriptionEndsAt()).isNotNull();

            verify(subscriptionRepository).save(subscription);
            verify(societyRepository).save(society);
        }
    }

    @Nested
    @DisplayName("activateSubscription")
    class ActivateSubscriptionTests {

        @Test
        @DisplayName("is idempotent and does nothing if subscription already SUCCESS")
        void activateSubscription_alreadySuccess_doesNothing() {
            subscription.setPaymentStatus(PaymentStatus.SUCCESS);

            subscriptionService.activateSubscription(subscription, "pay_123", "sig_123");

            verify(subscriptionRepository, never()).save(any());
            verify(societyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("changePlan")
    class ChangePlanTests {

        @Test
        @DisplayName("changing to same plan throws IllegalArgumentException")
        void changePlan_samePlan_throwsException() {
            society.setPlan(SubscriptionPlan.GROWTH);
            society.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            when(societyRepository.findById(SOCIETY_ID)).thenReturn(Optional.of(society));

            assertThatThrownBy(() -> subscriptionService.changePlan(SubscriptionPlan.GROWTH))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already on the GROWTH plan");
        }

        @Test
        @DisplayName("switching to TRIAL plan throws IllegalArgumentException")
        void changePlan_toTrial_throwsException() {
            society.setPlan(SubscriptionPlan.BASIC);
            society.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            when(societyRepository.findById(SOCIETY_ID)).thenReturn(Optional.of(society));

            assertThatThrownBy(() -> subscriptionService.changePlan(SubscriptionPlan.TRIAL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot switch to TRIAL plan");
        }

        @Test
        @DisplayName("changing plan from TRIAL or CANCELLED status throws IllegalArgumentException")
        void changePlan_fromTrialStatus_throwsException() {
            society.setPlan(SubscriptionPlan.BASIC);
            society.setSubscriptionStatus(SubscriptionStatus.TRIAL);
            when(societyRepository.findById(SOCIETY_ID)).thenReturn(Optional.of(society));

            assertThatThrownBy(() -> subscriptionService.changePlan(SubscriptionPlan.SCALE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot change plan from current subscription status");
        }

        @Test
        @DisplayName("valid plan change creates new order for new plan")
        void changePlan_valid_createsOrder() throws Exception {
            society.setPlan(SubscriptionPlan.BASIC);
            society.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            when(societyRepository.findById(SOCIETY_ID)).thenReturn(Optional.of(society));
            when(planPricingConfig.getAmountFor(SubscriptionPlan.SCALE)).thenReturn(499900L);

            JSONObject orderJson = new JSONObject();
            orderJson.put("id", "order_scale_1");
            when(orderClient.create(any(JSONObject.class))).thenReturn(new Order(orderJson));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderResponse response = subscriptionService.changePlan(SubscriptionPlan.SCALE);

            assertThat(response).isNotNull();
            assertThat(response.getRazorpayOrderId()).isEqualTo("order_scale_1");
            assertThat(response.getAmount()).isEqualTo(499900L);
        }
    }
}
