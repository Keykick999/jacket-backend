package com.example.ThirdProject.common.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final String ACCESS_COOKIE_NAME = "access-token";
    private final String TRACE_ID_KEY = "traceId";
    private final String USER_ID_KEY = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        // traceId 생성 및 MDC 저장
        String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID_KEY, traceId);

        log.info("[{}] Request 시작: {} {}", traceId, req.getMethod(), req.getRequestURI());

        try {
            resolveTokenFromCookie(req)
                    .flatMap(jwtService::getUserIdIfValid)
                    .ifPresent(userId -> {
                        MDC.put(USER_ID_KEY, userId);
                        this.setAuthentication(userId);
                    });

            res.setHeader("X-Trace-Id", traceId);

            chain.doFilter(req, res);

            log.info("[{}] Request 종료 완료", traceId);
        } catch (Exception e) {
            throw e;
        }
        finally {
            // 요청이 끝나면 MDC 비우기
            MDC.clear();
        }
    }

    private void setAuthentication(String userId) {
        UserDetails user = userDetailsService.loadUserByUsername(userId);
        var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Optional<String> resolveTokenFromCookie(HttpServletRequest req) {
        return Optional.ofNullable(req.getCookies())
                .stream()
                .flatMap(Arrays::stream)
                .filter(c -> ACCESS_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}