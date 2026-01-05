package com.example.ThirdProject.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class CookieUtil {   // TODO 배포 후에 쿠키들 도메인 설정 하기

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
                .domain(".xn--2y5bw4a.site")
                .build();
    }

    // 2. Refresh Token 쿠키 생성
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refresh-token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/") // refresh 요청시에만 서버로 전송되게 설정
                .maxAge(refreshTokenExpiration / 1000) // 밀리초 -> 초 변환
                .sameSite("None")   // TODO 배포후에 lax나 strict로 설정 변경하기
                .domain(".xn--2y5bw4a.site")
                .build();
    }

    // CSRF 토큰용 쿠키 (자바스크립트가 읽어야 함 -> httpOnly = false)
    public ResponseCookie createCsrfCookie() {
        String csrfToken = UUID.randomUUID().toString();

        return ResponseCookie.from("XSRF-TOKEN", csrfToken)
                .httpOnly(false) // 헤더에 담아야 해서 프론트가 읽을 수 있어야 됨
                .secure(true)    // https 쓰면 true
                .path("/")       // 전체 경로
                .maxAge(refreshTokenExpiration / 1000) // refresh token이랑 수명 맞춤 -> refresh시에 csrf토큰 없어서 에러나지 않게
                .sameSite("None")
                .domain(".xn--2y5bw4a.site")
                .build();
    }
}