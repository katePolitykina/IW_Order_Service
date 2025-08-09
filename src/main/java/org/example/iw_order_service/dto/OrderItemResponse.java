package org.example.iw_order_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemResponse {
    private Long id;
    private Long itemId;
    private Integer quantity;
}
