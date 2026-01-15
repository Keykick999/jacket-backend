package com.example.ThirdProject.common.csrf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.List;

public class CustomCsrfFilter extends OncePerRequestFilter {

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    private final List<String> URL_NOT_TO_FILTER = List.of(
            "/api/users/login",
            "/api/users/register",
            "/oauth2/**",
            "/login/oauth2/**",
            "/api/email/**"
    );

    private final List<String> ALLOWED_HTTP_METHODS = List.of("GET", "HEAD", "OPTIONS");

    // Spring의 경로 매칭 유틸리티 (/** 등 와일드카드 처리)
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        return URL_NOT_TO_FILTER.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestURI));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Safe Method (GET, HEAD, OPTIONS)는 통과
        if (ALLOWED_HTTP_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 헤더 및 쿠키에서 CSRF 토큰 추출
        String headerCsrfToken = request.getHeader(CSRF_HEADER_NAME);
        Cookie csrfCookie = WebUtils.getCookie(request, CSRF_COOKIE_NAME);
        String cookieCsrfToken = (csrfCookie != null) ? csrfCookie.getValue() : null;

        // 디버깅을 위한 로그 (필요 시 유지)
        // logger.debug(String.format("CSRF Header: %s, Cookie: %s", headerCsrfToken, cookieCsrfToken));

        // 3. 토큰 비교 검증
        if (isInvalidToken(headerCsrfToken, cookieCsrfToken)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF Token mismatch or missing");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isInvalidToken(String headerToken, String cookieToken) {
        return headerToken == null || cookieToken == null || !headerToken.equals(cookieToken);
    }
}