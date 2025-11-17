package com.example.ThirdProject.common.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String email, String purpose) {
        super("이메일 미인증: email=" + email + ", purpose=" + purpose);
    }
}