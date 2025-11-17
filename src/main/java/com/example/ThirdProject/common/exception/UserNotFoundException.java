package com.example.ThirdProject.common.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("회원을 찾을 수 없습니다.");
    }
}
