package com.example.ThirdProject.common.exception;

public class InvalidVerificationCodeException extends CustomException {
    public InvalidVerificationCodeException() {
        super(ErrorCode.INVALID_VERIFICATION_CODE);
    }
}