package com.example.ThirdProject.user.controller;

import com.example.ThirdProject.user.dto.*;
import com.example.ThirdProject.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest req) {
        String userId = auth.register(req);
        return ResponseEntity.created(java.net.URI.create("/api/users/" + userId)).build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(auth.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid RefreshRequest req) {
        return ResponseEntity.ok(auth.refresh(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid LogoutRequest req) {
        auth.logout(req);
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
}