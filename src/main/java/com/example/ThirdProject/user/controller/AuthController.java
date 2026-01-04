package com.example.ThirdProject.user.controller;

import com.example.ThirdProject.common.util.CookieUtil;
import com.example.ThirdProject.user.dto.*;
import com.example.ThirdProject.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest req) {
        String userId = authService.register(req);
        return ResponseEntity.created(java.net.URI.create("/api/users/" + userId)).build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody @Valid LoginRequest req) {
        TokenResponse tokens = authService.login(req);

        // access token, refresh token
        ResponseCookie accessCookie = cookieUtil.createAccessTokenCookie(tokens.accessToken());
        ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(tokens.refreshToken());

        // csrf token
        ResponseCookie csrfCookie = cookieUtil.createCsrfCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie.toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(value = "refresh-token", required = true) String refreshToken
    ) {
        TokenResponse tokens = authService.refresh(refreshToken);

        ResponseCookie accessCookie = cookieUtil.createAccessTokenCookie(tokens.accessToken());
        ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(tokens.refreshToken());
        ResponseCookie csrfCookie = cookieUtil.createCsrfCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie.toString())  // refresh시에 csrf토큰도 재발급
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid LogoutRequest req) {
        authService.logout(req);
        return ResponseEntity.noContent().build();
    }

    /**
       소셜 로그인 이메일 미제공 시 최종 회원가입 API
     */
    @PostMapping("/social-complete")
    public ResponseEntity<Map<String, String>> completeSocialRegistration(@RequestBody @Valid SocialCompleteRequest request) {
        authService.completeSocialRegistration(request);

        return new ResponseEntity<>(
                    Collections.singletonMap("message", "회원가입이 완료되었습니다."),
                    HttpStatus.CREATED);
    }

    /**
     * 계정 연동 확인 후 최종적으로 외부 Provider 정보를 DB에 링크하는 API
     */
    @PostMapping("/link-account")
    public ResponseEntity<UserLinkResponse> linkAccount(@RequestBody @Valid UserLinkRequest request) {
        UserLinkResponse response = authService.linkAccount(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    /**
     * 프론트엔드 인증 확인용 API
     * (헤더/쿠키의 토큰이 유효해야만 이 메서드에 도달함)
     */
    @GetMapping("/me")
    public ResponseEntity<Void> checkAuth() {
        // TODO: 나중에 회원 정보(ID, 이름, 프로필 등) 반환 로직 추가 필요.
        // 지금은 프론트에서 로그인 여부 판단을 위해서만 설정해뒀음.
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-id")
    public ResponseEntity<Void> checkId(@RequestParam("userId") String userId) {
        authService.checkUserIdAvailability(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/find-id")
    public ResponseEntity<Void> findId(@RequestBody @Valid FindIdRequest req) {
        authService.findId(req);
        return ResponseEntity.ok().build();
    }

    /**
     * 비밀번호 재설정: 이메일 인증이 완료된 후, 새로운 비밀번호로 변경
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok().build();
    }
}