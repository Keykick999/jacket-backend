package com.example.ThirdProject.common.exception;

public class DuplicateUserNameException extends RuntimeException {
    public DuplicateUserNameException(String userName) {
        super("Username already exists: " + userName);
    }
}