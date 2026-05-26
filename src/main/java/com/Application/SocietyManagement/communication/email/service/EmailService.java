package com.Application.SocietyManagement.communication.email.service;

import com.Application.SocietyManagement.communication.email.event.BillGeneratedEvent;
import com.Application.SocietyManagement.communication.email.event.PaymentSuccessEvent;
import com.Application.SocietyManagement.finance.entity.MaintenanceBill;
import com.Application.SocietyManagement.finance.enums.BillStatus;
import com.Application.SocietyManagement.finance.repository.MaintenanceBillRepository;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MaintenanceBillRepository billRepository;
    private final UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ── Event Listeners ──

    @Async
    @EventListener
    public void handleBillGenerated(BillGeneratedEvent event) {
        sendBillGeneratedEmail(event.getBill(), event.getResident());
    }

    @Async
    @EventListener
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        sendPaymentSuccessEmail(event.getBill(), event.getResident());
    }

    // ── Scheduled Notifications ──

    @Async
    public void sendInviteEmail(String to, String token, String role) {
        try {
            Context ctx = new Context();
            ctx.setVariable("token", token);
            ctx.setVariable("role", role);
            ctx.setVariable("acceptUrl",
                    "http://localhost:5173/accept-invite?token=" + token);
            ctx.setVariable("expiresIn", "48 hours");

            String html = templateEngine.process("email/invite", ctx);
            sendHtmlEmail(to, "You're invited to join CivicLink", html);
        } catch (Exception e) {
            log.error("Failed to send invite email to {}: {}", to, e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "UTC")
    public void sendBillReminders() {
        LocalDate threeDaysFromNow = LocalDate.now().plusDays(3);

        List<MaintenanceBill> dueSoonBills = billRepository
                .findByStatusAndDueDate(BillStatus.PENDING, threeDaysFromNow);

        dueSoonBills.forEach(bill ->
                userRepository.findById(bill.getUserId()).ifPresent(user ->
                        sendReminderEmail(bill, user)));

        log.info("Sent {} bill reminder emails", dueSoonBills.size());
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "UTC")
    public void sendOverdueNotifications() {
        List<MaintenanceBill> overdueBills = billRepository
                .findByStatusAndDueDateBefore(BillStatus.OVERDUE, LocalDate.now());

        overdueBills.forEach(bill ->
                userRepository.findById(bill.getUserId()).ifPresent(user ->
                        sendOverdueEmail(bill, user)));

        log.info("Sent {} overdue notification emails", overdueBills.size());
    }

    // ── Test Email ──

    public void sendTestEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("Test email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send test email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    // ── Private Email Senders ──

    @Async
    public void sendBillGeneratedEmail(MaintenanceBill bill, User resident) {
        try {
            Context ctx = new Context();
            ctx.setVariable("residentName", resident.getFirstName() + " " + resident.getLastName());
            ctx.setVariable("apartmentNumber", bill.getApartmentNumber());
            ctx.setVariable("billingMonth", bill.getBillingMonth());
            ctx.setVariable("dueDate", bill.getDueDate().toString());
            ctx.setVariable("amount", bill.getAmount());

            String html = templateEngine.process("email/bill-generated", ctx);
            sendHtmlEmail(resident.getEmail(),
                    "Maintenance Bill Generated - " + bill.getBillingMonth(), html);
        } catch (Exception e) {
            log.error("Failed to send bill generated email to {}: {}",
                    resident.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendReminderEmail(MaintenanceBill bill, User resident) {
        try {
            Context ctx = new Context();
            ctx.setVariable("residentName", resident.getFirstName() + " " + resident.getLastName());
            ctx.setVariable("apartmentNumber", bill.getApartmentNumber());
            ctx.setVariable("billingMonth", bill.getBillingMonth());
            ctx.setVariable("dueDate", bill.getDueDate().toString());
            ctx.setVariable("amount", bill.getAmount());

            String html = templateEngine.process("email/bill-reminder", ctx);
            sendHtmlEmail(resident.getEmail(),
                    "Payment Reminder - Bill Due in 3 Days", html);
        } catch (Exception e) {
            log.error("Failed to send reminder email to {}: {}",
                    resident.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendOverdueEmail(MaintenanceBill bill, User resident) {
        try {
            Context ctx = new Context();
            ctx.setVariable("residentName", resident.getFirstName() + " " + resident.getLastName());
            ctx.setVariable("apartmentNumber", bill.getApartmentNumber());
            ctx.setVariable("billingMonth", bill.getBillingMonth());
            ctx.setVariable("dueDate", bill.getDueDate().toString());
            ctx.setVariable("amount", bill.getAmount());

            String html = templateEngine.process("email/bill-overdue", ctx);
            sendHtmlEmail(resident.getEmail(),
                    "⚠️ Maintenance Bill Overdue - " + bill.getBillingMonth(), html);
        } catch (Exception e) {
            log.error("Failed to send overdue email to {}: {}",
                    resident.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendPaymentSuccessEmail(MaintenanceBill bill, User resident) {
        try {
            Context ctx = new Context();
            ctx.setVariable("residentName", resident.getFirstName() + " " + resident.getLastName());
            ctx.setVariable("apartmentNumber", bill.getApartmentNumber());
            ctx.setVariable("billingMonth", bill.getBillingMonth());
            ctx.setVariable("amount", bill.getAmount());
            ctx.setVariable("transactionId", bill.getUpiTransactionId());

            String html = templateEngine.process("email/payment-success", ctx);
            sendHtmlEmail(resident.getEmail(),
                    "✅ Payment Successful - " + bill.getBillingMonth(), html);
        } catch (Exception e) {
            log.error("Failed to send payment success email to {}: {}",
                    resident.getEmail(), e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String html)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
        log.info("Email sent to {} - subject: {}", to, subject);
    }
}
