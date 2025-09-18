package org.example.iw_order_service.dto;

import lombok.Data;
import org.example.iw_order_service.dto.enums.PaymentStatus;


import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private String id;
    private Long orderId;
    private Long userId;
    private LocalDateTime timestamp;
    private PaymentStatus status;
    private BigDecimal paymentAmount;
}
