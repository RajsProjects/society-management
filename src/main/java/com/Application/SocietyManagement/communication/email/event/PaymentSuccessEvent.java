package com.Application.SocietyManagement.communication.email.event;

import com.Application.SocietyManagement.finance.entity.MaintenanceBill;
import com.Application.SocietyManagement.users.entity.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PaymentSuccessEvent extends ApplicationEvent {
    private final MaintenanceBill bill;
    private final User resident;

    public PaymentSuccessEvent(Object source, MaintenanceBill bill, User resident) {
        super(source);
        this.bill = bill;
        this.resident = resident;
    }
}
