package com.example.ThirdProject.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    @Value("${jwt.access-exp-ms}")
    private int accessTokenExpiration;

    @Value("${jwt.refresh-exp-ms}")
    private int refreshTokenExpiration;

    // 1. Access Token 쿠키 생성
    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return ResponseCookie.from("access-token", accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(accessTokenExpiration / 1000) // 밀리초 -> 초 변환
                .sameSite("None")
                .build();
    }

    // 2. Refresh Token 쿠키 생성
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refresh-token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/api/users/refresh") // refresh 요청시에만 서버로 전송되게 설정
                .maxAge(refreshTokenExpiration / 1000) // 밀리초 -> 초 변환
                .sameSite("None")   // TODO 배포후에 lax나 strict로 설정 변경하기
                .build();
    }
}