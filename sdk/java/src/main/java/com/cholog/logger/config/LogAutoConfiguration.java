package com.cholog.logger.config;

import ch.qos.logback.classic.LoggerContext;
import com.cholog.logger.appender.CentralLogAppender;
import com.cholog.logger.filter.RequestBodyLoggingFilter;
import com.cholog.logger.filter.RequestResponseLoggingFilter;
import com.cholog.logger.filter.RequestTimingFilter;
import com.cholog.logger.service.LogSenderService;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CHO:LOG Logging Library의 자동 설정 클래스입니다.
 * Spring Boot의 자동 설정 메커니즘을 활용하여 필요한 Bean들을 자동으로 생성하고 등록합니다.
 *
 * 주요 기능:
 * - LogServerProperties 및 관련 설정 활성화
 * - LogSenderService 빈 생성
 * - CentralLogAppender 생성 및 Logback ROOT 로거에 등록
 * - HTTP 요청/응답을 로깅하기 위한 필터 등록
 * - 기본 CORS 설정 제공 (v1.8.6 추가)
 *
 * @author eddy1219
 * @version 1.8.6
 * @see com.cholog.logger.config.LogServerProperties
 * @see com.cholog.logger.service.LogSenderService
 * @see com.cholog.logger.appender.CentralLogAppender
 */
@AutoConfiguration // Spring Boot 2.7+ 에서 @Configuration + @AutoConfigureOrder + @AutoConfigureBefore/@AutoConfigureAfter 를 대체
@ConditionalOnClass({ch.qos.logback.classic.LoggerContext.class, ch.qos.logback.core.AppenderBase.class}) // Logback 클래스가 존재할 때만 활성화
@EnableConfigurationProperties({LogServerProperties.class})
@ConditionalOnProperty(prefix = "cholog.logger", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogAutoConfiguration {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LogAutoConfiguration.class);

    /**
     * 로그 전송 서비스 빈을 생성합니다.
     * 이 서비스는 로그 메시지를 배치로 모아 중앙 로그 서버로 전송하는 역할을 합니다.
     *
     * @param properties 로그 서버 접속 설정
     * @return 생성된 {@link LogSenderService} 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean
    public LogSenderService logSenderService(LogServerProperties properties) {
        log.info("CHO:LOG - Initializing LogSenderService bean...");
        // LogSenderService는 DisposableBean을 구현하므로, Spring이 종료 시 자동으로 destroy() 메소드를 호출하여 리소스를 정리합니다.
        return new LogSenderService(properties);
    }

    /**
     * Logback의 {@link ch.qos.logback.core.Appender} 구현체인 {@link CentralLogAppender}의 빈을 생성합니다.
     * 이 Appender는 애플리케이션에서 발생하는 로그 이벤트를 수집하여 {@link LogSenderService}로 전달하는 역할을 합니다.
     * 생성된 Appender는 Logback의 ROOT 로거에 자동으로 등록됩니다.
     * {@link ConditionalOnMissingBean}을 통해 사용자가 직접 {@link CentralLogAppender} 빈을 정의한 경우 이 빈은 생성되지 않습니다.
     *
     * @param logSenderService 로그 전송 서비스 (자동 주입)
     * @param properties       로그 서버 접속 설정 (자동 주입)
     * @param environment      Spring 환경 정보 (자동 주입, 프로필 등 컨텍스트 정보 로깅에 활용)
     * @return 생성 및 등록된 {@link CentralLogAppender} 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean
    public CentralLogAppender centralLogAppender(LogSenderService logSenderService, LogServerProperties properties, Environment environment) {
        log.info("CHO:LOG - Initializing and configuring CentralLogAppender bean.");
        CentralLogAppender appender = new CentralLogAppender(logSenderService, properties, environment);
        try {
            // 명시적으로 LoggerContext 확인
            ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
            if (!(loggerFactory instanceof LoggerContext)) {
                log.warn("CHO:LOG - LoggerFactory is not an instance of LoggerContext (actual: {}). " +
                         "CentralLogAppender could not be registered automatically. Manual configuration might be required.",
                         loggerFactory.getClass().getName());
                return appender;
            }
            
            LoggerContext loggerContext = (LoggerContext) loggerFactory;
            
            // 명시적으로 컨텍스트 및 이름 설정
            appender.setContext(loggerContext);
            appender.setName("CHOLOG_CENTRAL_APPENDER");
            
            // Appender 시작
            if (!appender.isStarted()) {
                appender.start();
                log.info("CHO:LOG - CentralLogAppender started successfully.");
            }
            
            // Root 로거에 Appender 추가
            ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            rootLogger.addAppender(appender);
            log.info("CHO:LOG - CentralLogAppender successfully registered to ROOT logger.");
        } catch (Exception e) {
            log.error("CHO:LOG - Failed to configure or register CentralLogAppender. Centralized logging may not work as expected.", e);
        }
        return appender;
    }

    /**
     * HTTP 요청의 시작과 끝을 감지하여 처리 시간을 측정하고, 고유 요청 ID(Request ID)를 생성하여
     * MDC(Mapped Diagnostic Context)에 저장하는 {@link RequestTimingFilter}의 빈을 생성합니다.
     * 이 필터는 다른 로깅 필터들보다 먼저 실행되어야 하며 ({@code Ordered.HIGHEST_PRECEDENCE}),
     * MDC에 저장된 정보는 후속 로그 이벤트에 자동으로 포함됩니다.
     * 서블릿 기반 웹 애플리케이션 환경 ({@link ConditionalOnWebApplication})에서만 생성됩니다.
     *
     * @param properties 로그 서버 접속 설정 (자동 주입)
     * @return 생성된 {@link RequestTimingFilter} 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public RequestTimingFilter requestTimingFilter(LogServerProperties properties) {
        log.info("CHO:LOG - Initializing RequestTimingFilter bean for Servlet environment.");
        return new RequestTimingFilter(properties);
    }
    
    /**
     * HTTP 요청 본문(body)을 로깅하기 위한 {@link RequestBodyLoggingFilter}의 빈을 생성합니다.
     * 이 필터는 {@link org.springframework.web.util.ContentCachingRequestWrapper}를 사용하여 요청 본문을 캐시하며,
     * {@code cholog.logger.request-body-logging=true} (기본값)일 때 활성화됩니다.
     * 서블릿 기반 웹 애플리케이션 환경에서만 생성됩니다.
     *
     * @return 생성된 {@link RequestBodyLoggingFilter} 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "cholog.logger", name = "request-body-logging", havingValue = "true", matchIfMissing = true)
    public RequestBodyLoggingFilter requestBodyLoggingFilter() {
        log.info("CHO:LOG - Initializing RequestBodyLoggingFilter bean for request body logging.");
        return new RequestBodyLoggingFilter();
    }
    
    /**
     * HTTP 요청 및 응답의 상세 정보(헤더, 파라미터, 응답 본문 등)를 로깅하는
     * {@link RequestResponseLoggingFilter}의 빈을 생성합니다.
     * 이 필터는 {@code cholog.logger.request-response-logging=true} (기본값)일 때 활성화됩니다.
     * 서블릿 기반 웹 애플리케이션 환경에서만 생성됩니다.
     *
     * @param properties 로그 서버 접속 설정 (자동 주입)
     * @return 생성된 {@link RequestResponseLoggingFilter} 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "cholog.logger", name = "request-response-logging", havingValue = "true", matchIfMissing = true)
    public RequestResponseLoggingFilter requestResponseLoggingFilter(LogServerProperties properties) {
        log.info("CHO:LOG - Initializing RequestResponseLoggingFilter bean for detailed request/response logging.");
        return new RequestResponseLoggingFilter(properties);
    }
    
    /**
     * 기본 CORS 설정을 제공하는 CorsFilter 빈을 생성합니다.
     * 모든 출처(*)에 대해 기본적인 CORS 허용 설정을 구성합니다.
     * {@code cholog.logger.cors-enabled=true} 일 때 활성화됩니다.
     * 이미 CorsFilter 빈이 등록되어 있는 경우 이 빈은 생성되지 않습니다.
     * 
     * @return CORS 필터
     */
//    @Bean
//    @ConditionalOnMissingBean(CorsFilter.class)
//    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
//    @ConditionalOnProperty(prefix = "cholog.logger", name = "cors-enabled", havingValue = "true", matchIfMissing = false)
//    public CorsFilter corsFilter() {
//        log.info("CHO:LOG - Initializing default CORS configuration");
//        CorsConfiguration config = new CorsConfiguration();
//        config.addAllowedOrigin("*"); // 모든 오리진 허용
//        config.addAllowedHeader("*"); // 모든 헤더 허용
//        config.addAllowedMethod("*"); // 모든 HTTP 메소드 허용
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config); // 모든 경로에 적용
//
//        return new CorsFilter(source);
//    }

    /**
     * CHO:LOG SDK에서 사용하는 X-Request-Id 헤더를 모든 CORS 설정에 자동으로 추가하기 위한
     * BeanPostProcessor를 등록합니다.
     * 이 Processor는 UrlBasedCorsConfigurationSource 타입의 빈을 찾아
     * X-Request-Id를 허용 및 노출 헤더에 추가합니다.
     *
     * @return CorsConfigurationCustomizingBeanPostProcessor 인스턴스
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "cholog.logger", name = "cors-enabled", havingValue = "true", matchIfMissing = false)
    public static CorsConfigurationCustomizingBeanPostProcessor chologCorsConfigurationCustomizer() {
        // static @Bean 메소드로 선언하여 BeanFactoryPostProcessor 단계 이후,
        // 일반 빈들이 초기화되기 전에 이 BeanPostProcessor가 등록되도록 합니다.
        log.info("CHO:LOG - Registering CorsConfigurationCustomizingBeanPostProcessor to handle X-Request-Id in CORS configurations.");
        return new CorsConfigurationCustomizingBeanPostProcessor();
    }
}