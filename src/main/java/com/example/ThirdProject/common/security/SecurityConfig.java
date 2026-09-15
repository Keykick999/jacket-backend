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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)   // 프론트엔드(React)와 API 통신을 하므로, 스프링이 제공하는 기본 로그인 페이지(HTML)가 필요 없음.
                .httpBasic(AbstractHttpConfigurer::disable)   // 로그인 로직을 Controller(/users/login)에 직접 구현했기 때문에 스프링의 기본 필터(UsernamePasswordAuthenticationFilter)를 사용하지 않음.
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth // TODO 나중에 필요한 경로만 설정하는 식으로 바꾸기
                        .requestMatchers(
                                "/api/users/**",
                                "/api/email/**",
                                "/email/**",
                                "/login/oauth2/code/**",
                                "/oauth2/**",
                                "/error",
                                "/favicon.ico",
                                "/uploads/**",
                                "/api/admin/test/**",

                                // Actuator
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/metrics",
                                "/actuator/prometheus",
                                "/actuator/threaddump",
                                "/test/**"
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

    // CORS 설정을 별도 빈으로 분리하여 안전하게 관리
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 1. 허용할 Origin 설정
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                frontendUrl,
                "https://xn--2y5bw4a.site"
        ));

        // 2. 허용할 HTTP 메서드 설정 (중요: OPTIONS가 있어야 Preflight 통과)
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 3. 허용할 헤더 설정
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Cache-Control",
                "x-requested-with",
                "X-XSRF-TOKEN"
        ));

        // 4. 내보낼 헤더 설정 (프론트에서 JWT 등을 읽어야 할 때 필요)
        config.setExposedHeaders(Arrays.asList("Authorization", "X-XSRF-TOKEN"));

        // 5. 쿠키 허용
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
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