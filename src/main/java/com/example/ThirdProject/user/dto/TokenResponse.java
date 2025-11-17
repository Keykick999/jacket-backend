package com.example.ThirdProject.user.dto;

public record TokenResponse(String accessToken, long expiresInMs, String refreshToken) {}