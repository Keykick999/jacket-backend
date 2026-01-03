package com.example.ThirdProject.common.exception;

public class EmailNotVerifiedException extends CustomException {
    public EmailNotVerifiedException() {
        super(ErrorCode.EMAIL_NOT_VERIFIED);
    }
}