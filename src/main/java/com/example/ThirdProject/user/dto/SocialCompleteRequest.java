package com.example.ThirdProject.user.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.ToString;

@Data
@ToString
public class SocialCompleteRequest {

    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank
    private String nickname;

    @NotBlank
    private String providerId; // 소셜 서비스에서 받은 고유 ID

    @NotBlank
    private String provider; // 소셜 서비스 이름 (e.g., kakao, naver)
}