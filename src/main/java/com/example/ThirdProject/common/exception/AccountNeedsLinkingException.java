package com.example.ThirdProject.common.exception;

import com.example.ThirdProject.common.oauth.OAuth2FailureData;
import org.springframework.security.core.AuthenticationException;

public class AccountNeedsLinkingException extends AuthenticationException {

    private final OAuth2FailureData data;

    public AccountNeedsLinkingException(OAuth2FailureData data, String msg) {
        super(msg);
        this.data = data;
    }

    public OAuth2FailureData getData() {
        return data;
    }
}