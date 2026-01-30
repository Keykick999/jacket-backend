package com.example.ThirdProject.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.ToString;

public record SendCodeRequest(
        @Email @NotBlank String email,
        @NotBlank EmailPurpose purpose // "signup" / "reset" 등
) {}