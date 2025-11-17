package com.example.ThirdProject.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 공통 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력 값이 올바르지 않습니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 리소스입니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),

    // 게시글 좋아요 관련 에러
    POST_ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요를 누른 게시글입니다."),
    POST_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요한 게시글을 찾을 수 없습니다."),

    // 댓글 좋아요 관련 에러
    COMMENT_ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요를 누른 댓글입니다."),
    COMMENT_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요한 댓글을 찾을 수 없습니다."),

    // 이메일 에러
    EMAIL_DUPLICATE_FOR_LINKING(HttpStatus.CONFLICT, "입력하신 이메일은 이미 등록된 계정입니다. 연동을 진행해주세요."),
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 사용 중인 이메일입니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
