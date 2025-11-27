package com.example.ThirdProject.common.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserDetailsService uds;

    public JwtAuthenticationFilter(JwtService jwt, UserDetailsService uds) {
        this.jwt = jwt; this.uds = uds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        // 1. 쿠키에서 토큰 추출
        String token = resolveTokenFromCookie(req);

        // 2. 토큰 유효성 검사
        if (token != null && jwt.isTokenValid(token)) {
            String userId = jwt.getUserId(token);
            UserDetails user = uds.loadUserByUsername(userId);
            var at = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(at);
        }

        chain.doFilter(req, res);
    }


    /**
     * HttpServletRequest의 쿠키들 중에서 "access-token"을 찾아서 값을 반환
     */
    private String resolveTokenFromCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie c : cookies) {
            // CookieUtil에서 설정한 이름("access-token")과 일치하는 쿠키 찾기
            if ("access-token".equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}