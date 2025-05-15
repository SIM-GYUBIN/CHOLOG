package com.cholog.logger.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cholog Logger SDK의 자동 설정을 담당합니다.
 * 웹 애플리케이션 환경에서 GlobalExceptionHandler를 자동으로 빈으로 등록합니다.
 *
 * @version 1.0.3
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET) // 서블릿 기반 웹 애플리케이션일 때만 활성화
public class ChologLoggerAutoConfiguration {

    /**
     * 전역 예외 처리를 위한 GlobalExceptionHandler 빈을 등록합니다.
     * 이 핸들러는 요청 attribute에 저장된 requestId를 사용하여 로그를 남깁니다.
     * 
     * @return GlobalExceptionHandler 인스턴스
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
} 