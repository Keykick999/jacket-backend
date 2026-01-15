package com.example.ThirdProject.common.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final String ACCESS_COOKIE_NAME = "access-token";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        resolveTokenFromCookie(req)
                .flatMap(jwtService::getUserIdIfValid)
                .ifPresent(this::setAuthentication);

        chain.doFilter(req, res);
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