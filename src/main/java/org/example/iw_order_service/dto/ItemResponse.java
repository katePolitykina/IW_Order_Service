package org.example.iw_order_service.dto;


import lombok.Data;

import java.math.BigDecimal;
@Data

public class ItemResponse {
    private Long id;
    private String name;
    private BigDecimal price;
}
