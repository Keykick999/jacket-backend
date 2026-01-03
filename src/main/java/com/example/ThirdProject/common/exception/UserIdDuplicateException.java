package com.example.ThirdProject.common.exception;

public class UserIdDuplicateException extends CustomException {
    public UserIdDuplicateException() {
        super(ErrorCode.DUPLICATE_RESOURCE); // "이미 존재하는 리소스입니다." (409)
    }
}