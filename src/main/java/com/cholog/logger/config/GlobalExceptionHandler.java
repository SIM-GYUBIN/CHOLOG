package com.cholog.logger.config;

import com.cholog.logger.appender.CentralLogAppender;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 전역 예외 처리 클래스.
 * Spring 애플리케이션에서 발생하는 모든 {@link Exception}을 처리하여,
 * {@link com.cholog.logger.appender.CentralLogAppender#REQUEST_ID_MDC_KEY}에서 가져온 요청 ID를 포함하여
 * 일관된 형식으로 에러 로그를 기록하고 클라이언트에게 표준화된 응답을 반환합니다.
 * <p>
 * 이 핸들러는 {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE} 우선순위를 가져
 * 다른 예외 처리기보다 먼저 실행되도록 구성됩니다.
 * <p>
 * {@link ChologLoggerAutoConfiguration}에 의해 자동으로 빈으로 등록될 수 있습니다.
 *
 * @version 1.0.3
 * @see ChologLoggerAutoConfiguration
 * @see com.cholog.logger.filter.RequestTimingFilter
 * @see com.cholog.logger.appender.CentralLogAppender
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE) // 다른 예외 처리기보다 먼저 실행되도록 순서 지정
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 모든 {@link Exception} 타입의 예외를 처리합니다.
     * <p>
     * 1. {@link WebRequest}에서 {@link HttpServletRequest}를 추출하고, 요청 URI와 요청 attribute에 저장된
     *    {@link CentralLogAppender#REQUEST_ID_MDC_KEY} 값을 가져옵니다.
     * 2. 예외 정보(errorId, type, path, message), HTTP 상태 코드(500), 그리고 추출한 요청 ID를 MDC에 설정합니다.
     * 3. {@code requestId}를 포함하여 에러 로그를 기록합니다.
     * 4. 클라이언트에게 반환할 표준화된 오류 응답 {@link ResponseEntity}를 생성합니다. 이 응답에는
     *    errorId, requestId, 상태 코드, 메시지 등이 포함됩니다.
     *
     * @param ex 발생한 예외 객체
     * @param webRequest 현재 웹 요청 객체
     * @return 클라이언트에게 반환될 {@link ResponseEntity}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest webRequest) {
        String requestIdFromAttribute = null;
        String requestUri = null;
        
        if (webRequest instanceof ServletWebRequest servletWebRequest) {
            HttpServletRequest httpRequest = servletWebRequest.getRequest();
            requestUri = httpRequest.getRequestURI();
            Object requestIdAttr = httpRequest.getAttribute(CentralLogAppender.REQUEST_ID_MDC_KEY);
            if (requestIdAttr instanceof String) {
                requestIdFromAttribute = (String) requestIdAttr;
            }
        }

        String errorId = UUID.randomUUID().toString();
        
        // MDC에 정보 설정
        MDC.put("error_id", errorId);
        MDC.put("error_type", ex.getClass().getName());
        if (requestUri != null) {
            MDC.put("error_path", requestUri);
        }
        MDC.put("error_message", ex.getMessage());
        MDC.put(CentralLogAppender.HTTP_STATUS_MDC_KEY, String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()));

        // RequestId 처리: attribute에 있으면 사용, 없으면 MDC에 있는 값 (다른 필터가 설정했을 수도 있음) 확인
        String effectiveRequestId = MDC.get(CentralLogAppender.REQUEST_ID_MDC_KEY);
        if (requestIdFromAttribute != null) {
            if (effectiveRequestId == null || !effectiveRequestId.equals(requestIdFromAttribute)) {
                MDC.put(CentralLogAppender.REQUEST_ID_MDC_KEY, requestIdFromAttribute);
                effectiveRequestId = requestIdFromAttribute; // MDC 업데이트 및 로깅용 변수 업데이트
            }
        }
        // effectiveRequestId가 여전히 null이면, UUID 등으로 새로 생성할 수도 있으나,
        // RequestTimingFilter에서 반드시 설정하므로 null이 아니어야 함.

        log.error("GlobalExceptionHandler handled exception. Error ID: {}, Request ID: [{}], URI: [{}]",
                errorId, effectiveRequestId, requestUri, ex);

        // 클라이언트 응답 구성
        Map<String, Object> body = new HashMap<>();
        body.put("errorId", errorId);
        if (effectiveRequestId != null) {
            body.put("requestId", effectiveRequestId);
        }
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please contact support with the Error ID and Request ID if available.");
        // body.put("path", requestUri); // 필요시 경로 포함

        // RequestTimingFilter의 finally에서 모든 MDC를 정리하므로 여기서 개별적으로 제거할 필요는 없음
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 