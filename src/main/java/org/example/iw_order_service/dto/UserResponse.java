package org.example.iw_order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String surname;
    private String email;
}
