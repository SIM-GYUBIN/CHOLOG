package com.cholog.logger.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.cholog.logger.appender.CentralLogAppender;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 전역 예외 처리 클래스
 * 
 * 애플리케이션 전체에서 발생하는 예외를 처리하고 로깅합니다.
 * 이 클래스는 다음과 같은 역할을 합니다:
 * 1. 모든 예외를 잡아서 구조화된 응답 형식으로 변환
 * 2. 각 예외에 고유 식별자(UUID) 부여하여 클라이언트에게 제공
 * 3. 발생한 예외의 상세 정보 로깅
 * 4. 예외 정보를 MDC에 저장하여 CentralLogAppender가 로그에 예외 정보를 포함하도록 함
 *
 * v1.8.6 변경사항:
 * - X-Request-Id 헤더를 통한 프론트엔드-백엔드 요청 추적 통합 지원
 *
 * v1.8.4 변경사항:
 * - HTTP 상태 코드 500을 MDC에 명시적으로 설정하여 모든 예외 로그에 상태 코드 포함
 * - Tomcat 로그에서도 상태 코드가 표시되도록 개선
 * 
 * v1.8.3 변경사항:
 * - 예외 처리 후 MDC에서 에러 정보를 제거하지 않도록 개선하여 로그에 예외 정보 포함
 * - RequestTimingFilter에서 모든 MDC 정보를 정리하도록 통합
 * 
 * @version 1.8.6
 */
@ControllerAdvice
@Order(0) // 다른 ControllerAdvice 클래스보다 먼저 실행되도록 높은 우선순위 지정
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 모든 예외를 처리하는 핸들러 메서드
     * 
     * @param ex 발생한 예외
     * @param request 현재 요청 객체
     * @param webRequest 웹 요청 컨텍스트
     * @return 표준화된 오류 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(
            Exception ex, HttpServletRequest request, WebRequest webRequest) {
        
        // 각 예외에 고유 식별자(에러 ID) 부여
        String errorId = UUID.randomUUID().toString();
        
        // 로깅을 위한 MDC 컨텍스트에 에러 정보 추가
        MDC.put("error_id", errorId);
        MDC.put("error_type", ex.getClass().getName());
        MDC.put("error_path", request.getRequestURI());
        MDC.put("error_message", ex.getMessage());
        
        // 중요: 모든 처리되지 않은 예외는 HTTP 500 상태 코드를 발생시키므로 
        // CentralLogAppender.HTTP_STATUS_MDC_KEY에 명시적으로 500 설정
        MDC.put(CentralLogAppender.HTTP_STATUS_MDC_KEY, "500");
        
        // 에러 로그 기록 (스택 트레이스 포함)
        log.error("Uncaught exception: {} at URI: {} with error_id: {}", 
                ex.getMessage(), request.getRequestURI(), errorId, ex);
        
        // MDC에서 에러 정보를 제거하지 않음 (RequestTimingFilter가 나중에 로그를 기록할 때 사용할 수 있도록)
        // 나중에 RequestTimingFilter의 finally 블록에서 모든 MDC 정보를 정리함
        
        // 클라이언트에게 반환할 응답 구성
        Map<String, Object> body = new HashMap<>();
        body.put("errorId", errorId);
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "서버 오류가 발생했습니다. 지원팀에 문의 시 에러 ID를 알려주세요.");
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 