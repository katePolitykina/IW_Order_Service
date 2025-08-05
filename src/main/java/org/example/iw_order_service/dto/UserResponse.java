package org.example.iw_order_service.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Data
@RequiredArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String surname;
    private String email;
}
