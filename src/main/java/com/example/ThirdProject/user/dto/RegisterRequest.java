package com.example.ThirdProject.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String userId,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank @Size(min = 2, max = 50) String userName,
        @NotBlank @Size(min = 2, max = 50) String nickname,
        @Size(min = 2, max = 50) String phone,
        @Email @NotBlank String email
) {}
