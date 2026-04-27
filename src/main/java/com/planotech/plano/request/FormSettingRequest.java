package com.planotech.plano.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FormSettingRequest {
    private Boolean paid;
    private BigDecimal amount;
    private String currency;
    private LocalDate paymentDeadline;
}
