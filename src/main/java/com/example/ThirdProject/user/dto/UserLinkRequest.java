package com.example.ThirdProject.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLinkRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String providerId;

    @NotBlank
    private String provider;
}