package org.example.iw_order_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemRequest {
    @NotNull
    private Long itemId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
