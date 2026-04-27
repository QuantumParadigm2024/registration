package com.planotech.plano.response;

import com.planotech.plano.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor

public class PaymentUserDTO {
    private String name;
    private String email;
    private String phone;
    private String type;
    private PaymentStatus status;
    private BigDecimal amount;
    private LocalDateTime paidAt;

    private String paymentId;

}
