package com.example.ThirdProject.common.oauth;

import com.example.ThirdProject.common.exception.AccountNeedsLinkingException;
import com.example.ThirdProject.common.exception.AdditionalInfoRequiredException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final String FRONTEND_ERROR_CALLBACK_URL = frontendUrl + "/oauth/error";
    private final String ADDITIONAL_SIGNUP_URL = frontendUrl + "/signup/additional";;
    private final String LINK_CONFIRM_URL = frontendUrl + "/link-confirm"; // 연동 확인 페이지

    // 예외 체인을 탐색하여 원하는 커스텀 예외 객체를 찾아 반환합니다.
    private <T extends AuthenticationException> T getRequiredException(Throwable t, Class<T> targetClass) {
        if (t == null) {
            return null;
        }
        if (targetClass.isInstance(t)) {
            return targetClass.cast(t);
        }
        // 원인(cause)을 재귀적으로 확인
        return getRequiredException(t.getCause(), targetClass);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        // 1. [Provider ID 추출] URI 경로에서 registrationId (provider name)를 안전하게 추출
        String registrationId;
        try {
            String uri = request.getRequestURI();
            registrationId = uri.substring(uri.lastIndexOf('/') + 1);
        } catch (Exception e) {
            registrationId = "unknown";
        }

        // 2. [예외 탐색] 두 가지 커스텀 예외 중 어떤 것이 발생했는지 확인합니다.
        AdditionalInfoRequiredException infoException = getRequiredException(exception, AdditionalInfoRequiredException.class);
        AccountNeedsLinkingException linkingException = getRequiredException(exception, AccountNeedsLinkingException.class);


        // 3. 🚨 [분기 1: 계정 연동 필요] (가장 중요한 보안 플로우)
        if (linkingException != null) {
            OAuth2FailureData failureData = linkingException.getData();

            // 연동 확인 페이지로 리다이렉트
            String finalUrl = UriComponentsBuilder.fromUriString(LINK_CONFIRM_URL)
                    .queryParam("providerId", failureData.getProviderId())
                    .queryParam("provider", registrationId)
                    .queryParam("existingEmail", failureData.getExistingEmail())
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();

            getRedirectStrategy().sendRedirect(request, response, finalUrl);
            return;
        }

        // 4. 📧 [분기 2: 이메일 누락]
        if (infoException != null) {
            // 이메일 미동의 시, 추가 정보 입력 페이지로 리다이렉트합니다.
            OAuth2FailureData failureData = infoException.getData();

            String finalUrl = UriComponentsBuilder.fromUriString(ADDITIONAL_SIGNUP_URL)
                    .queryParam("provider", registrationId)
                    .queryParam("nickname", failureData.getNickname())
                    .queryParam("providerId", failureData.getProviderId())
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();

            getRedirectStrategy().sendRedirect(request, response, finalUrl);
            return;
        }


        // 5. ❌ 기타 모든 실패 (LOGIN_FAILED, invalid_client 등) 처리
        String errorCode = "LOGIN_FAILED";
        if (exception instanceof OAuth2AuthenticationException) {
            errorCode = ((OAuth2AuthenticationException) exception).getError().getErrorCode();
        }

        String finalUrl = UriComponentsBuilder.fromUriString(FRONTEND_ERROR_CALLBACK_URL)
                .queryParam("error", errorCode)
                .build().encode(StandardCharsets.UTF_8)
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, finalUrl);
    }
}