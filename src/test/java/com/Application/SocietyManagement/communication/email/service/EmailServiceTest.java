package com.Application.SocietyManagement.communication.email.service;

import com.Application.SocietyManagement.communication.email.event.BillGeneratedEvent;
import com.Application.SocietyManagement.communication.email.event.PaymentSuccessEvent;
import com.Application.SocietyManagement.finance.entity.MaintenanceBill;
import com.Application.SocietyManagement.finance.enums.BillStatus;
import com.Application.SocietyManagement.finance.repository.MaintenanceBillRepository;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MaintenanceBillRepository billRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmailService emailService;

    private User resident;
    private MaintenanceBill bill;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@societymanagement.com");

        resident = User.builder()
                .email("resident@example.com")
                .firstName("Alice")
                .lastName("Smith")
                .build();
        resident.setId("user-100");

        bill = MaintenanceBill.builder()
                .userId("user-100")
                .apartmentNumber("B-204")
                .billingMonth("OCT-2026")
                .amount(BigDecimal.valueOf(3500.0))
                .dueDate(LocalDate.now().plusDays(3))
                .status(BillStatus.PENDING)
                .upiTransactionId("UPI-12345")
                .build();
    }

    @Test
    @DisplayName("handleBillGenerated renders template and sends mime email")
    void handleBillGenerated_success() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/bill-generated"), any(Context.class))).thenReturn("<p>Bill Generated</p>");

        BillGeneratedEvent event = new BillGeneratedEvent(this, bill, resident);
        emailService.handleBillGenerated(event);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("handlePaymentSuccess renders template and sends mime email")
    void handlePaymentSuccess_success() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/payment-success"), any(Context.class))).thenReturn("<p>Payment Success</p>");

        PaymentSuccessEvent event = new PaymentSuccessEvent(this, bill, resident);
        emailService.handlePaymentSuccess(event);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendTestEmail sends simple email")
    void sendTestEmail_success() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertThatCode(() -> emailService.sendTestEmail("test@example.com", "Test Subject", "Test Body"))
                .doesNotThrowAnyException();

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendBillReminders finds due bills and dispatches emails")
    void sendBillReminders_success() {
        LocalDate threeDaysFromNow = LocalDate.now().plusDays(3);
        when(billRepository.findByStatusAndDueDate(BillStatus.PENDING, threeDaysFromNow))
                .thenReturn(List.of(bill));
        when(userRepository.findById("user-100")).thenReturn(Optional.of(resident));

        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/bill-reminder"), any(Context.class))).thenReturn("<p>Reminder</p>");

        emailService.sendBillReminders();

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendOverdueNotifications finds overdue bills and dispatches overdue emails")
    void sendOverdueNotifications_success() {
        bill.setStatus(BillStatus.OVERDUE);
        when(billRepository.findByStatusAndDueDateBefore(eq(BillStatus.OVERDUE), any(LocalDate.class)))
                .thenReturn(List.of(bill));
        when(userRepository.findById("user-100")).thenReturn(Optional.of(resident));

        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/bill-overdue"), any(Context.class))).thenReturn("<p>Overdue</p>");

        emailService.sendOverdueNotifications();

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendInviteEmail creates and dispatches invite template email")
    void sendInviteEmail_success() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/invite"), any(Context.class))).thenReturn("<p>Invite</p>");

        emailService.sendInviteEmail("invitee@example.com", "token123", "RESIDENT");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("skips sending if mailSender is null")
    void sendHtmlEmail_nullSender_skipsWithoutException() {
        EmailService serviceWithoutSender = new EmailService(null, templateEngine, billRepository, userRepository);
        ReflectionTestUtils.setField(serviceWithoutSender, "fromEmail", "noreply@societymanagement.com");

        BillGeneratedEvent event = new BillGeneratedEvent(this, bill, resident);
        assertThatCode(() -> serviceWithoutSender.handleBillGenerated(event))
                .doesNotThrowAnyException();
    }
}
