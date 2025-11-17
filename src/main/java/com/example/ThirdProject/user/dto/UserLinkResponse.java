package com.example.ThirdProject.user.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 계정 연동 성공 시 JWT 토큰을 포함하여 반환하는 응답 DTO
 */
@Data
@Builder
public class UserLinkResponse {
    private String message;
    private String accessToken;
    private String userUuid;
}