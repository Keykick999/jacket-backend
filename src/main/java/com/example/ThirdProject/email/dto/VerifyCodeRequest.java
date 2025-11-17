package com.example.ThirdProject.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyCodeRequest(
        @Email @NotBlank String email,
        @NotBlank String purpose,
        @NotBlank @Size(min = 6, max = 6) String code
) {}
