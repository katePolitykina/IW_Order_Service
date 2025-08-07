package org.example.iw_order_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderItemRequest {
    @NotNull
    private Long itemId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
