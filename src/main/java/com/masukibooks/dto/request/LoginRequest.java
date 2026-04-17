package com.masukibooks.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String identifier;  // email or phone

    @NotBlank
    private String password;
}
