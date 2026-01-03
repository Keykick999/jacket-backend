package com.example.ThirdProject.common.exception;


public class InvalidRequestException extends CustomException {
    public InvalidRequestException(String message) {
        super(ErrorCode.INVALID_INPUT_VALUE, message);
    }
}