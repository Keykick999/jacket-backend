package com.example.ThirdProject.common.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessExpMs;
    private final long refreshExpMs;

    public JwtService(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.access-exp-ms}") long accessExpMs,
            @Value("${jwt.refresh-exp-ms}") long refreshExpMs
    ) {
        this.key = Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(base64Secret));
        this.accessExpMs = accessExpMs;
        this.refreshExpMs = refreshExpMs;
    }

    public String generateAccessToken(UserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())              // jti (추후 로그아웃 블랙리스트용 확장 가능)
                .setSubject(user.getUsername())
                .addClaims(Map.of("roles", user.getAuthorities()))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(accessExpMs)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateAccessToken(String userId) {
        // 권한이 필요 없다면 빈 리스트, 필요하다면 List.of(new SimpleGrantedAuthority("ROLE_USER")) 등을 사용
        return generateAccessToken(userId, new ArrayList<>());
    }

    public String generateAccessToken(String userId, Collection<? extends GrantedAuthority> authorities) {
        Instant now = Instant.now();

        // 권한 정보를 클레임에 넣기 좋게 변환 (필요 시)
        // List<String> roles = authorities.stream()
        //        .map(GrantedAuthority::getAuthority)
        //        .collect(Collectors.toList());

        return Jwts.builder()
                .setId(UUID.randomUUID().toString()) // JWT 자체의 고유 ID (jti)
                .setSubject(userId)                  // ★ 여기가 중요! (User의 UUID가 들어감)
                //.claim("roles", roles)             // 권한 정보가 필요하면 추가
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(accessExpMs)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("typ", "refresh")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(refreshExpMs)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId)
                .claim("typ", "refresh")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(refreshExpMs)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isRefresh(String token) {
        try {
            return "refresh".equals(parse(token).getBody().get("typ"));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUserId(String token) {
        return parse(token).getBody().getSubject();
    }

    public Date getExpiration(String token) {
        return parse(token).getBody().getExpiration();
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

    public Optional<String> getUserIdIfValid(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            Claims body = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Optional.ofNullable(body.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty(); // 만료, 잘못된 서명 등 모든 예외를 empty로 처리
        }
    }
}