package com.cholog.logger.filter;

import com.cholog.logger.appender.CentralLogAppender;
import com.cholog.logger.config.LogServerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servlet Filter 구현체로, 라이브러리의 핵심 기능 중 하나인 HTTP 요청 자동 추적 및 로깅을 담당합니다.
 * 주요 역할:
 * 들어오는 모든 HTTP 요청의 시작과 끝 시간을 측정하여 처리 시간(Response Time)을 계산합니다.
 * 각 요청마다 고유한 요청 ID(Request ID)를 생성합니다.
 * X-Request-Id 헤더가 있는 경우 해당 값을 요청 ID로 사용합니다 (프론트엔드-백엔드 통합 추적 지원).
 * 요청의 상세 정보(HTTP Method, URI, Client IP, User-Agent)를 추출합니다.
 * 요청 처리가 완료된 후 최종 HTTP 응답 상태 코드(Status Code)를 가져옵니다.
 * 위에서 수집된 주요 컨텍스트 정보(요청 ID 포함)를 SLF4j MDC(Mapped Diagnostic Context)와 {@link HttpServletRequest} attribute에 저장하여, 후속 로깅 처리(예: `CentralLogAppender`에서의 `requestId` 참조)나 다른 사용자 정의 필터/핸들러에서 활용될 수 있도록 합니다.
 * 요청 처리가 완료되는 시점에 INFO 레벨의 로그를 자동으로 기록하여, {@link CentralLogAppender}가 해당 요청의 모든 컨텍스트 정보를 포함한 로그를 중앙 서버로 전송하도록 트리거합니다.
 * 요청 처리가 완전히 끝나면 해당 스레드의 MDC에서 추가했던 모든 정보를 정리합니다.
 * 이 필터는 {@link com.cholog.logger.config.LogAutoConfiguration}에 의해 서블릿 기반 웹 애플리케이션 환경에서
 * 자동으로 등록되며, {@link Ordered#HIGHEST_PRECEDENCE} 우선순위를 가져 필터 체인의 가장 앞단에서 실행됩니다.
 *
 * v1.8.6 변경사항:
 * - 프론트엔드에서 전달된 X-Request-Id 헤더가 있는 경우 해당 값을 요청 ID로 사용하도록 기능 추가
 * - 이를 통해 프론트엔드와 백엔드 간 요청 추적 통합 및 마이크로서비스 환경에서의 요청 흐름 추적 지원
 *
 * v1.8.4 변경사항:
 * - Tomcat 컨테이너 로그에서도 상태 코드(500)가 표시되도록 개선
 * - 예외 정보가 발생한 경우 상태 코드를 자동으로 추론하여 MDC에 추가
 * - 로그 일관성을 위해 상태 코드 처리 로직 개선
 *
 * v1.8.3 변경사항:
 * - 요청 처리 중 예외 발생 시 예외 정보를 MDC에 저장하여 로그에 예외 정보 포함
 * - MDC 정리 과정에서 모든 관련 필드(예외 정보, 에러 정보, 요청 파라미터, 헤더 등)를 제거하도록 개선
 *
 * @version 1.0.3
 * @see CentralLogAppender (MDC 키 상수 공유)
 * @see com.cholog.logger.config.LogAutoConfiguration
 * @see MDC
 */
@Order(Ordered.HIGHEST_PRECEDENCE) // 다른 필터보다 먼저 실행되어 전체 요청 시간을 감싸도록 순서 지정
public class RequestTimingFilter implements Filter {

    /** 필터 자체의 로그 기록(특히 요청 완료 자동 로그)을 위한 Logger */
    private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);
    
    /** 로깅에서 제외해야 할 민감 파라미터 키워드 목록 */
    private static final String[] SENSITIVE_PARAMETER_KEYWORDS = {
        "password", "pwd", "secret", "token", "auth", "key", "apikey", "api-key", "credential", 
        "card", "credit", "cvv", "cvc", "pin", "ssn", "social", "sin", "tax", "fiscal", 
        "passport", "license", "national", "identity", "private"
    };
    
    /** 로그 서버 설정 */
    private final LogServerProperties properties;
    
    /**
     * 생성자를 통한 LogServerProperties 주입
     * 
     * @param properties 로그 서버 설정
     */
    public RequestTimingFilter(LogServerProperties properties) {
        this.properties = properties;
    }

    /**
     * 서블릿 필터의 핵심 로직을 수행하는 메소드입니다.
     * 요청 시작 시 컨텍스트 정보(ID, HTTP 상세)를 MDC와 {@link HttpServletRequest} attribute에 설정하고, {@code chain.doFilter}를 호출하여
     * 실제 요청 처리를 수행합니다. 요청에 X-Request-Id 헤더가 있는 경우 해당 값을 요청 ID로 사용하고,
     * 없는 경우에만 UUID를 생성하여 사용합니다. 이를 통해 프론트엔드와 백엔드 간 통합 요청 추적이 가능합니다.
     * 요청 처리가 완료되면(성공 또는 예외 발생 무관하게 finally 블록 실행),
     * 응답 시간 및 상태 코드를 계산/획득하여 MDC에 추가한 후, INFO 레벨의 요약 로그를 기록합니다.
     * 마지막으로 현재 요청 처리를 위해 MDC에 추가했던 모든 정보를 반드시 제거합니다.
     *
     * @param request  서블릿 컨테이너로부터 전달받은 요청 객체
     * @param response 서블릿 컨테이너로부터 전달받은 응답 객체
     * @param chain    다음 필터 또는 서블릿으로 요청/응답을 전달하기 위한 필터 체인 객체
     * @throws IOException      요청 처리 중 IO 관련 예외 발생 시
     * @throws ServletException 요청 처리 중 서블릿 관련 예외 발생 시
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        long startTime = System.nanoTime(); // 정밀한 시간 측정을 위해 nanoTime 사용
        String requestId = UUID.randomUUID().toString(); // 고유 요청 ID 생성
        // 요청/응답 객체가 HttpServlet 타입인지 미리 확인 (캐스팅 안전성 및 MDC 정리 로직 위해)
        boolean isHttpServletRequest = request instanceof HttpServletRequest;
        boolean isHttpServletResponse = response instanceof HttpServletResponse;
        boolean mdcPopulated = false; // MDC에 이 필터가 값을 넣었는지 추적
        Integer statusCode = null;    // 최종 HTTP 상태 코드 저장 변수

        try {
            // --- 1. 요청 시작 시 MDC 설정 ---
            MDC.put(CentralLogAppender.REQUEST_ID_MDC_KEY, requestId);
            if (isHttpServletRequest) {
                ((HttpServletRequest) request).setAttribute(CentralLogAppender.REQUEST_ID_MDC_KEY, requestId);
            }
            mdcPopulated = true; // 최소한 requestId는 설정됨

            String method = null;
            String uri = null;
            // HTTP 요청일 경우 상세 정보 추출 및 MDC 저장
            if (isHttpServletRequest) {
                HttpServletRequest httpServletRequest = (HttpServletRequest) request;
                
                // X-Request-Id 헤더가 있는 경우 이를 사용하고, 없는 경우에만 UUID 생성
                String headerRequestId = httpServletRequest.getHeader("X-Request-Id");
                if (headerRequestId != null && !headerRequestId.trim().isEmpty()) {
                    // 프론트엔드에서 전달한 X-Request-Id 사용
                    requestId = headerRequestId.trim();
                    // MDC에 설정된 값 갱신
                    MDC.put(CentralLogAppender.REQUEST_ID_MDC_KEY, requestId);
                }
                
                method = httpServletRequest.getMethod();
                uri = httpServletRequest.getRequestURI();
                MDC.put(CentralLogAppender.REQUEST_METHOD_MDC_KEY, method);
                MDC.put(CentralLogAppender.REQUEST_URI_MDC_KEY, uri);
                MDC.put(CentralLogAppender.REQUEST_CLIENT_IP_MDC_KEY, request.getRemoteAddr());
                String userAgent = httpServletRequest.getHeader("User-Agent");
                MDC.put(CentralLogAppender.REQUEST_USER_AGENT_MDC_KEY, userAgent);

                // request header를 평평하게 MDC에 추가 (이중 JSON 방지)
                httpServletRequest.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                    String value = httpServletRequest.getHeader(headerName);
                    if (value != null) {
                        MDC.put("request_header_" + headerName, value);
                    }
                });

                // 중요 헤더 정보 정확하게 MDC에 추가
                Map<String, String> requestHeaders = new HashMap<>();
                httpServletRequest.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                    requestHeaders.put(headerName, httpServletRequest.getHeader(headerName));
                });
                
                // JSON 형태로 전체 헤더 정보를 request_headers에 저장
                try {
                    MDC.put("request_headers", new ObjectMapper().writeValueAsString(requestHeaders));
                } catch (Exception e) {
                    log.warn("Failed to convert request headers to JSON", e);
                    MDC.put("request_headers", "{\"error\":\"Failed to serialize headers\"}");
                }

                // request parameter를 평평하게 MDC에 추가
                httpServletRequest.getParameterMap().forEach((key, values) -> {
                    if (isSensitiveParameter(key)) {
                        // 민감 정보로 판단된 파라미터는 마스킹 처리
                        MDC.put("request_param_" + key, properties.getSensitiveValueReplacement());
                    } else {
                        MDC.put("request_param_" + key, String.join(",", values));
                    }
                });
            }
            // --------------------------

            // --- 2. 실제 요청 처리 위임 ---
            // 다음 필터 또는 DispatcherServlet 등으로 제어 전달
            chain.doFilter(request, response);
            // --------------------------

            // --- 3. 정상 완료 후 상태 코드 가져오기 ---
            // 예외 없이 여기까지 오면 정상 처리 완료. 응답 객체에서 상태 코드 획득 시도.
            if (isHttpServletResponse) {
                statusCode = ((HttpServletResponse) response).getStatus();
            }
            // ---------------------------------

        } catch (Exception e) {
            // 요청 처리 중 예외 발생 시 처리
            // 예외 정보를 MDC에 저장하고 상태 코드를 500으로 설정(처리되지 않은 예외)
            if (mdcPopulated) {
                MDC.put("exception_class", e.getClass().getName());
                MDC.put("exception_message", e.getMessage());
                
                // 예외가 발생했으므로 상태 코드를 500으로 설정 
                // (SpringMVC의 예외 처리기에서 변경될 수 있으나 기본값으로 500 설정)
                MDC.put(CentralLogAppender.HTTP_STATUS_MDC_KEY, "500");
                statusCode = 500;
            }
            
            throw e;
        } finally {
            // --- 4. 요청 처리 완료 후 항상 실행 (성공/예외 무관) ---
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000; // 처리 시간 계산 (ms)

            // finally 블록 시작 시점의 상태 코드 재확인 (예외 발생 등으로 try 블록에서 못 얻었을 수 있음)
            if (isHttpServletResponse && statusCode == null) {
                // 응답 객체가 커밋되지 않았다면 상태 코드 가져오기 가능
                try {
                    statusCode = ((HttpServletResponse) response).getStatus();
                } catch (Exception e) {
                    // 응답 객체 상태 이상 등으로 상태 코드 얻기 실패 시 로깅 (선택적)
                    log.warn("Could not get response status code in finally block.", e);
                }
            }

            try {
                // --- 5. 최종 컨텍스트 정보 MDC에 설정 ---
                // (mdcPopulated 플래그는 requestId라도 설정되었으면 true)
                if (mdcPopulated) {
                    // 상태 코드 MDC에 추가 (null이 아닐 경우)
                    if (statusCode != null) {
                        MDC.put(CentralLogAppender.HTTP_STATUS_MDC_KEY, String.valueOf(statusCode));
                    } else {
                        // 상태 코드가 여전히 null이면 MDC에서 기존 값 확인
                        String existingStatus = MDC.get(CentralLogAppender.HTTP_STATUS_MDC_KEY);
                        
                        // MDC에 상태 코드가 없고 에러/예외 정보가 있는 경우 500으로 설정
                        if (existingStatus == null && 
                            (MDC.get("error_id") != null || MDC.get("exception_class") != null)) {
                            MDC.put(CentralLogAppender.HTTP_STATUS_MDC_KEY, "500");
                            statusCode = 500;
                        }
                    }
                    
                    // 응답 시간 MDC에 추가
                    MDC.put(CentralLogAppender.RESPONSE_TIME_MDC_KEY, String.valueOf(durationMs));

                    // --- 6. 자동 로그 기록 (Appender 트리거) ---
                    // 이 로그 이벤트는 위에서 MDC에 넣은 모든 정보를 포함한 상태로 발생함
                    // CentralLogAppender는 이 이벤트를 받아 최종 로그 데이터를 구성함
                    try {
                        // INFO 레벨로 요청 처리 완료 로그 기록
                        log.info("Request Finished: {} {} status={} duration={}ms requestId={}",
                                MDC.get(CentralLogAppender.REQUEST_METHOD_MDC_KEY), // MDC에서 정보 가져오기
                                MDC.get(CentralLogAppender.REQUEST_URI_MDC_KEY),
                                statusCode, // 상태 코드 (null일 경우 자동으로 "null"로 표시됨)
                                durationMs,
                                requestId // try 블록에서 생성한 ID 사용
                        );
                    } catch (Exception e) {
                        // 자동 로그 기록 자체에서 오류 발생 시 (거의 없어야 함)
                        log.error("Error occurred during automatic request finished logging.", e);
                    }
                    // ---------------------------------------

                } // if(mdcPopulated) 끝
            } finally {
                // --- 7. 현재 요청에서 사용된 모든 MDC 키 제거 ---
                // (스레드 재사용 시 이전 요청 정보가 남지 않도록 반드시 정리)
                if (mdcPopulated) {
                    MDC.remove(CentralLogAppender.RESPONSE_TIME_MDC_KEY);
                    MDC.remove(CentralLogAppender.HTTP_STATUS_MDC_KEY);
                    MDC.remove(CentralLogAppender.REQUEST_ID_MDC_KEY);
                    if (isHttpServletRequest) { // 추가했던 HTTP 관련 키들도 제거
                        MDC.remove(CentralLogAppender.REQUEST_METHOD_MDC_KEY);
                        MDC.remove(CentralLogAppender.REQUEST_URI_MDC_KEY);
                        MDC.remove(CentralLogAppender.REQUEST_CLIENT_IP_MDC_KEY);
                        MDC.remove(CentralLogAppender.REQUEST_USER_AGENT_MDC_KEY);
                        
                        // request header 및 parameter 관련 필드 제거
                        MDC.remove("request_headers");
                    }
                    
                    // 예외 정보 관련 필드 제거
                    MDC.remove("exception_class");
                    MDC.remove("exception_message");
                    
                    // GlobalExceptionHandler에서 추가한 에러 필드 제거
                    MDC.remove("error_id");
                    MDC.remove("error_type");
                    MDC.remove("error_path");
                    MDC.remove("error_message");

                    // 모든 request_param_ 및 request_header_ 접두사를 가진 MDC 항목 제거
                    MDC.getCopyOfContextMap().keySet().stream()
                            .filter(key -> key.startsWith("request_param_") || key.startsWith("request_header_"))
                            .forEach(MDC::remove);
                }
                // ---------------------------------------
            }
        }
    }

    private boolean isSensitiveParameter(String key) {
        for (String keyword : SENSITIVE_PARAMETER_KEYWORDS) {
            if (key.toLowerCase().contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}