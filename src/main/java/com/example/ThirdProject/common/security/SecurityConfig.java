package com.example.ThirdProject.common.security;

import com.example.ThirdProject.common.csrf.CustomCsrfFilter;
import com.example.ThirdProject.common.jwt.JwtAuthenticationEntryPoint;
import com.example.ThirdProject.common.jwt.JwtAuthenticationFilter;
import com.example.ThirdProject.common.oauth.CustomOAuth2UserService;
import com.example.ThirdProject.common.oauth.OAuth2AuthenticationFailureHandler;
import com.example.ThirdProject.common.oauth.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import java.util.Collections;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    @Value("${app.frontend.url}")
    private String frontendUrl;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(Arrays.asList(
                            "http://localhost:5173", // 로컬 개발용 유지
                            frontendUrl              // Vercel 주소
                    ));
                    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Cache-Control"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)   // 프론트엔드(React)와 API 통신을 하므로, 스프링이 제공하는 기본 로그인 페이지(HTML)가 필요 없음.
                .httpBasic(AbstractHttpConfigurer::disable)   // 로그인 로직을 Controller(/users/login)에 직접 구현했기 때문에 스프링의 기본 필터(UsernamePasswordAuthenticationFilter)를 사용하지 않음.
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth // TODO 나중에 필요한 경로만 설정하는 식으로 바꾸기
                        .requestMatchers(
                                "/",
                                "/users/login",
                                "/users/social-complete", // Explicitly allow this
                                "/api/users/social-complete", // Also allow with /api prefix just in case
                                "/api/users/**", // Allow all /api/users for now to be safe
                                "/api/email/**",
                                "/email/**",
                                "/login/oauth2/code/**",
                                "/oauth2/**",
                                "/error",
                                "/favicon.ico",
                                "/uploads/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new CustomCsrfFilter(), JwtAuthenticationFilter.class)
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

