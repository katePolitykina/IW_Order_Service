package org.example.iw_order_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PaymentRequest {
    @NotNull
    private Long orderId;
    @NotNull
    private Long userId;
    @NotNull
    @Positive
    private BigDecimal paymentAmount;

}
