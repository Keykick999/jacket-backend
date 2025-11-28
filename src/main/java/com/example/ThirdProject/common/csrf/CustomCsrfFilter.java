package com.example.ThirdProject.common.csrf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CustomCsrfFilter extends OncePerRequestFilter {

    // CSRF 검사를 생략할 URL 리스트 (로그인, 회원가입 등)
    private static final List<String> EXCLUDE_URLS = Arrays.asList(
            "/users/login",
            "/users/signup", // 회원가입이 있다면 추가
            "/oauth2/",      // OAuth2 관련 요청은 별도 필터가 처리하거나 리다이렉트임
            "/login/oauth2/"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // 제외 리스트에 포함된 경로면 필터 실행 안 함 (true 반환)
        return EXCLUDE_URLS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // 1. GET, HEAD, OPTIONS는 안전하니까 검사 안 함
        String method = req.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            chain.doFilter(req, res);
            return;
        }

        // 2. 쿠키에서 CSRF 토큰 찾기
        String csrfCookieValue = null;
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if ("XSRF-TOKEN".equals(c.getName())) {
                    csrfCookieValue = c.getValue();
                    break;
                }
            }
        }

        // 3. 헤더에서 CSRF 토큰 찾기
        String csrfHeaderValue = req.getHeader("X-XSRF-TOKEN");

        System.out.println("csrfCookieValue = " + csrfCookieValue);
        System.out.println("csrfHeaderValue = " + csrfHeaderValue);

        // 4. 비교 검증 (쿠키가 없거나, 헤더가 없거나, 둘이 다르면 차단)
        if (csrfCookieValue == null || csrfHeaderValue == null || !csrfCookieValue.equals(csrfHeaderValue)) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.setContentType("text/plain;charset=UTF-8"); // 한글 깨짐 방지
            res.getWriter().write("CSRF Attack Detected! (Custom Filter)");
            return;
        }

        // 5. 통과
        chain.doFilter(req, res);
    }
}