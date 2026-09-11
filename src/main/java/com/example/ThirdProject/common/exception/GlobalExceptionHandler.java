package com.example.ThirdProject.common.exception;
import com.example.ThirdProject.slack.SlackNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.util.ContentCachingRequestWrapper;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final SlackNotificationService slackNotificationService;

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e, HttpServletRequest request) {
        String traceId = Optional.ofNullable(MDC.get("traceId")).orElse("none");
        String userId = Optional.ofNullable(MDC.get("userId")).orElse("anonymous");
        String requestBody = getRequestBody(request);

        ErrorCode errorCode = e.getErrorCode();
        StackTraceElement location = e.getStackTrace()[0];

        // 로그에 고유 코드 대신 Enum 상수명(예: PROJECT_NOT_FOUND)을 출력하여 식별력 확보
        log.warn("[{}] 비즈니스 예외 발생\n" +
                        "User: {}\n" +
                        "Location: {}.{}(Line:{})\n" +
                        "ErrorName: {}, Message: {}\n" +
                        "Request Body: {}",
                traceId, userId,
                location.getClassName(), location.getMethodName(), location.getLineNumber(),
                errorCode.name(), e.getDetailMessage(),
                requestBody);

        slackNotificationService.sendErrorNotification(traceId, userId, e, request);

        ErrorResponse response = new ErrorResponse(
                errorCode.getStatus().value(),
                e.getDetailMessage(),
                traceId
        );
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        String traceId = Optional.ofNullable(MDC.get("traceId")).orElse("none");
        String userId = Optional.ofNullable(MDC.get("userId")).orElse("anonymous");

        String requestBody = getRequestBody(request);

        // 에러 발생 위치 정보
        StackTraceElement location = e.getStackTrace()[0];

        // 종합 로그 출력 (TraceID, 유저, 위치, 바디 정보 포함)
        log.error("[{}] 서버 내부 오류 발생\n" +
                        "User: {}\n" +
                        "Location: {}.{}(Line:{})\n" +
                        "Request Body: {}\n" +
                        "Exception: ",
                traceId, userId,
                location.getClassName(), location.getMethodName(), location.getLineNumber(),
                requestBody, e);

        slackNotificationService.sendErrorNotification(traceId, userId, e, request);

        ErrorResponse response = new ErrorResponse(500, "서버 내부 오류가 발생했습니다.", traceId);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public record ErrorResponse(int status, String message, String traceId) {
    }

    // 바디를 문자열로 변환하는 헬퍼 메서드
    private String getRequestBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            // [중요] 만약 캐시가 비어있다면, getContentAsByteArray는 아무것도 주지 않습니다.
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                try {
                    return new String(buf, 0, buf.length, wrapper.getCharacterEncoding());
                } catch (Exception e) {
                    return "Error parsing body";
                }
            }
        }
        return "Body not cached - check Filter chain";
    }
}