package com.example.ThirdProject.common.security;

import com.example.ThirdProject.common.jwt.JwtAuthenticationFilter;
import com.example.ThirdProject.common.oauth.CustomOAuth2UserService;
import com.example.ThirdProject.common.oauth.OAuth2AuthenticationFailureHandler;
import com.example.ThirdProject.common.oauth.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;



@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 3개의 OAuth2 관련 컴포넌트 주입
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())

                // [필수] 세션 STATELESS 설정 (이미 되어있음)
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        // [수정] OAuth2 콜백 엔드포인트는 인증 없이 허용해야 함
                        .requestMatchers(
                                "/", "/users", "/users/login", "/email/**", "/carTest", "/**", "/edit-post", // 기존 URL
                                "/login/oauth2/code/**", // OAuth2 콜백
                                "/oauth2/**" // (혹시 모를) OAuth2 관련 경로
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        // 1. 로그인 성공 시 사용할 핸들러
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        // 2. 로그인 실패 시 사용할 핸들러
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                        // 3. (핵심) OAuth2 유저 정보를 처리할 커스텀 서비스 등록
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                )

                // JWT 필터 추가 (이미 되어있음)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}