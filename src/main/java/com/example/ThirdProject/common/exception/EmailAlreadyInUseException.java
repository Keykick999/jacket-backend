package com.example.ThirdProject.common.exception;

/**
 * 이메일이 이미 다른 사용자에 의해 사용 중일 때 발생하는 커스텀 예외입니다.
 * (소셜 로그인 최종 등록 시 중복 이메일 체크 용도)
 */
public class EmailAlreadyInUseException extends CustomException {

    // RuntimeException을 상속받아 트랜잭션 롤백을 유발합니다.
    public EmailAlreadyInUseException() {
        super(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

}