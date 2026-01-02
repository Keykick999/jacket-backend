package com.example.ThirdProject.common.oauth;

import com.example.ThirdProject.common.jwt.JwtService;
import com.example.ThirdProject.common.jwt.TokenStore;
import com.example.ThirdProject.common.util.CookieUtil;
import com.example.ThirdProject.user.entity.User;
import com.example.ThirdProject.user.entity.UserProvider;
import com.example.ThirdProject.user.repository.UserProviderRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtService jwtService;
    private final UserProviderRepository userProviderRepository;
    private final TokenStore tokenStore;
    private final CookieUtil cookieUtil;
    @Value("${app.frontend.url}")
    private String frontendUrl;

//    private final String FRONTEND_CALLBACK_URL = frontendUrl +"/oauth/callback";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // 1. Principal에서 Provider ID (Social ID)와 Provider 이름(kakao, naver)을 추출합니다.
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;

        String providerName = authToken.getAuthorizedClientRegistrationId(); // 'kakao', 'naver' 등

        OAuthAttributes attributes = OAuthAttributes.of(
                providerName,
                oAuth2User.getName(), // Spring Security가 Principal Name으로 설정한 값 (e.g., Kakao's 'id')
                oAuth2User.getAttributes()
        );

        // 2. 획득한 Provider ID와 Provider 이름을 사용합니다.
        String providerId = attributes.getProviderId();

        // UserProvider 테이블을 조회하여 User 엔티티를 획득합니다.
        User user = userProviderRepository
                .findByProviderAndProviderId(providerName, providerId)
                .map(UserProvider::getUser) // 연동 기록에서 User FK를 가져옵니다.
                .orElseThrow(() -> new IllegalStateException("FATAL: User link not found after successful authentication."));

        // JWT 토큰 생성 및 발행
        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // Refresh Token 저장
        long refreshTtl = jwtService.getExpiration(refreshToken).getTime() - System.currentTimeMillis();
        tokenStore.saveRefresh(user.getId(), refreshToken, refreshTtl);

        // 쿠키 생성
        ResponseCookie accessCookie = cookieUtil.createAccessTokenCookie(accessToken);
        ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(refreshToken);
        ResponseCookie csrfCookie = cookieUtil.createCsrfCookie();

        // 응답 헤더에 쿠키 추가 (이 시점에 브라우저에 저장 명령이 내려감)
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, csrfCookie.toString());

        // 프론트엔드로 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/oauth/callback") // 경로를 안전하게 붙여줍니다.
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}