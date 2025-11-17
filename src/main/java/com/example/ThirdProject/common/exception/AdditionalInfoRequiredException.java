package com.example.ThirdProject.common.exception;

import com.example.ThirdProject.common.oauth.OAuth2FailureData;
import org.springframework.security.core.AuthenticationException;

/**
 * 이메일 미동의 등으로 소셜 로그인 최종 등록에 추가 정보가 필요할 때 발생.
 * 데이터 Payload를 포함하여 FailureHandler로 전달됨.
 */
public class AdditionalInfoRequiredException extends AuthenticationException {

    private final OAuth2FailureData data;

    public AdditionalInfoRequiredException(OAuth2FailureData data) {
        super("ADDITIONAL_INFO_REQUIRED"); // 고정된 Error Message
        this.data = data;
    }

    public OAuth2FailureData getData() {
        return data;
    }
}