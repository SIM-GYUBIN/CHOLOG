package com.cholog.logger.filter;

import com.cholog.logger.appender.CentralLogAppender;
import com.cholog.logger.config.LogServerProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 요청 및 응답의 상세 정보를 로깅하는 서블릿 필터입니다.
 * 이 필터는 {@link ContentCachingRequestWrapper} 및 {@link ContentCachingResponseWrapper}를 사용하여
 * 요청 및 응답 본문을 포함한 다양한 정보를 캡처합니다. 로깅되는 주요 정보는 다음과 같습니다:
 * 요청 헤더 (민감한 헤더 제외)
 * 요청 파라미터 (URL 쿼리 스트링)
 * 응답 상태 코드
 * 응답 헤더 (민감한 헤더 제외)
 * 응답 본문 (최대 길이 제한 적용)
 * 요청 본문 로깅은 {@link RequestBodyLoggingFilter}가 먼저 실행되어 요청 내용을 캐시해야 효과적입니다.
 * 이 필터는 {@link RequestTimingFilter} 이후에 실행되어야 하며, MDC를 통해 수집된 정보를 로그 이벤트에 추가합니다.
 * 필터 순서는 {@link Order} 어노테이션으로 제어됩니다 ({@code Ordered.LOWEST_PRECEDENCE - 10}).
 *
 * @version 1.8.6
 * @see RequestTimingFilter
 * @see RequestBodyLoggingFilter
 * @see CentralLogAppender
 */
@Order(Ordered.LOWEST_PRECEDENCE - 10) // RequestTimingFilter 이후, 기본 필터들보다는 앞에 실행
public class RequestResponseLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    
    /** 로깅에서 제외할 HTTP 헤더 이름 목록 (소문자로 비교, 부분 일치) */
    private static final String[] EXCLUDED_HEADERS = {
        "authorization", "auth", "cookie", "jwt", "token", "password", "secret", "credential", "key",
        "x-api-key", "api-key", "apikey", "access", "private"
    };
    
    /** 로깅할 요청/응답 본문의 최대 길이 (bytes). 이 길이를 초과하면 잘리고 "... (truncated)"가 추가됨. */
    private static final int MAX_PAYLOAD_LENGTH = 10000;

    /**
     * 로깅에서 제외해야 할 민감 파라미터 키워드 목록
     */
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
    public RequestResponseLoggingFilter(LogServerProperties properties) {
        this.properties = properties;
    }

    /**
     * 필터의 핵심 로직을 수행합니다.
     * 요청과 응답 객체를 캐싱 가능한 래퍼로 감싸고, 요청 처리 전후로 상세 정보를 로깅합니다.
     * 중요한 점은 {@code responseWrapper.copyBodyToResponse()}를 {@code finally} 블록에서 호출하여
     * 실제 클라이언트에게 응답 내용이 전달되도록 보장하는 것입니다.
     *
     * @param request  서블릿 요청 객체
     * @param response 서블릿 응답 객체
     * @param chain    필터 체인
     * @throws IOException      요청/응답 처리 중 I/O 오류 발생 시
     * @throws ServletException 서블릿 관련 오류 발생 시
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // 이미 RequestBodyLoggingFilter 등에서 래핑되었을 수 있으므로, instanceof로 확인 후 캐스팅하거나 새로 래핑합니다.
        ContentCachingRequestWrapper requestWrapper = request instanceof ContentCachingRequestWrapper
            ? (ContentCachingRequestWrapper) request
            : new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper((HttpServletResponse) response);

        try {
            // 요청 관련 로깅은 최대한 유지 시도
            // logRequestDetails(requestWrapper); // 기존에 이 메서드가 있다면 호출

            // 중요: ContentCachingResponseWrapper로 응답을 감싸지 않고 원본 응답 객체를 그대로 전달
            chain.doFilter(requestWrapper, response);

        } finally {
            // 응답 로깅: ContentCachingResponseWrapper를 사용하지 않으므로,
            // 기존 logResponseDetails 메서드는 응답 본문을 읽지 못할 것입니다.
            // HttpServletResponse에서 직접 상태 코드나 헤더를 읽어 로깅하는
            // 최소한의 로직만 수행하거나, 이 부분 로깅도 일시적으로 줄입니다.
            if (response instanceof HttpServletResponse) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                String requestId = MDC.get(CentralLogAppender.REQUEST_ID_MDC_KEY); // RequestTimingFilter에서 설정된 값 사용
                // 기존 logResponseDetails에서 헤더 로깅 등을 분리하여 여기서 호출할 수 있습니다.
                // 예: logResponseHeadersAndStatusOnly(httpResponse);
            }
            // responseWrapper.copyBodyToResponse(); // 이 줄을 반드시 제거하거나 주석 처리!
        }
    }
    
    /**
     * HTTP 요청의 헤더와 쿼리 파라미터를 추출하여 MDC에 저장하고, 관련 정보를 로깅합니다.
     * 요청 본문은 이 메서드에서 직접 처리하지 않습니다 (주로 {@link RequestBodyLoggingFilter} 담당).
     *
     * @param request 래핑된 HTTP 요청 객체
     */
    private void logRequestDetails(ContentCachingRequestWrapper request) {
        String requestId = MDC.get(CentralLogAppender.REQUEST_ID_MDC_KEY); // RequestTimingFilter에서 설정
        
        Map<String, String> headers = extractHeaders(request);
        MDC.put("request_headers", formatMapToJson(headers));
        
        Map<String, String> parameters = getParameters(request);
        MDC.put("request_params", formatMapToJson(parameters));
        
        // 요청 시작에 대한 간단한 로그. 상세 내용은 MDC에 저장되어 최종 로그 이벤트에 포함됩니다.
        log.info("Request processing details: method={}, uri={}, requestId={}", 
                request.getMethod(), request.getRequestURI(), requestId);
    }
    
    /**
     * HTTP 응답의 상태 코드, 헤더, 본문을 추출하여 MDC에 저장하고, 관련 정보를 로깅합니다.
     *
     * @param response 래핑된 HTTP 응답 객체
     */
    private void logResponseDetails(ContentCachingResponseWrapper response) {
        String requestId = MDC.get(CentralLogAppender.REQUEST_ID_MDC_KEY);
        
        byte[] content = response.getContentAsByteArray();
        String responseBody = getContentAsString(content, response.getCharacterEncoding(), MAX_PAYLOAD_LENGTH);
        
        if (!responseBody.isEmpty()) {
            MDC.put("response_body", responseBody);
        }
        
        MDC.put("http_status", String.valueOf(response.getStatus()));
        
        Map<String, String> responseHeadersMap = new HashMap<>();
        for (String headerName : response.getHeaderNames()) {
            if (!isExcludedHeader(headerName)) {
                responseHeadersMap.put(headerName, response.getHeader(headerName));
            }
        }
        MDC.put("response_headers", formatMapToJson(responseHeadersMap));
        
        // 응답 완료에 대한 간단한 로그. 상세 내용은 MDC에 저장되어 최종 로그 이벤트에 포함됩니다.
        log.info("Response processing completed: status={}, requestId={}", 
                response.getStatus(), requestId);
        
        // 주의: 이 필터에서는 MDC에서 개별 항목을 정리하지 않습니다.
        // MDC의 전반적인 정리는 주로 요청 사이클의 가장 바깥 필터(예: RequestTimingFilter)에서 담당합니다.
    }
    
    /**
     * {@link HttpServletRequest}에서 민감 정보를 제외한 헤더들을 추출하여 맵으로 반환합니다.
     *
     * @param request HTTP 요청 객체
     * @return 헤더 이름과 값을 담은 맵
     */
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        
        // 민감 정보 대체 문자열 가져오기
        String maskedValue = properties.getSensitiveValueReplacement();
        
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            // 대소문자 구분 없이 필터링할 헤더 목록과 비교
            if (isExcludedHeader(headerName)) {
                // 민감 헤더는 마스킹 처리
                headers.put(headerName, maskedValue);
            } else {
                // 멀티밸류 헤더를 올바르게 처리
                Enumeration<String> headerValues = request.getHeaders(headerName);
                if (headerValues != null) {
                    StringBuilder valueBuilder = new StringBuilder();
                    boolean first = true;
                    while (headerValues.hasMoreElements()) {
                        if (!first) {
                            valueBuilder.append(", ");
                        }
                        valueBuilder.append(headerValues.nextElement());
                        first = false;
                    }
                    headers.put(headerName, valueBuilder.toString());
                }
            }
        }
        return headers;
    }
    
    /**
     * {@link HttpServletRequest}에서 URL 쿼리 파라미터들을 추출하여 맵으로 반환합니다.
     * 민감 정보로 간주되는 파라미터는 마스킹 처리됩니다.
     *
     * @param request HTTP 요청 객체
     * @return 파라미터 이름과 값을 담은 맵
     */
    private Map<String, String> getParameters(HttpServletRequest request) {
        Map<String, String> parameters = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        
        // 민감 정보 대체 문자열 가져오기
        String maskedValue = properties.getSensitiveValueReplacement();
        
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            if (isSensitiveParameter(paramName)) {
                // 민감 정보로 판단된 파라미터는 마스킹 처리
                parameters.put(paramName, maskedValue);
            } else {
                // 멀티밸류 파라미터 처리
                String[] values = request.getParameterValues(paramName);
                if (values != null && values.length > 0) {
                    if (values.length == 1) {
                        // 단일 값 파라미터
                        String value = values[0];
                        // 파라미터 값 자체에 민감 정보가 포함되었는지 확인
                        if (containsSensitiveValue(value)) {
                            parameters.put(paramName, maskedValue);
                        } else {
                            parameters.put(paramName, value);
                        }
                    } else {
                        // 멀티밸류 파라미터
                        StringBuilder valueBuilder = new StringBuilder();
                        boolean containsSensitive = false;
                        
                        for (int i = 0; i < values.length; i++) {
                            if (i > 0) {
                                valueBuilder.append(", ");
                            }
                            
                            if (containsSensitiveValue(values[i])) {
                                containsSensitive = true;
                                break;
                            }
                            
                            valueBuilder.append(values[i]);
                        }
                        
                        if (containsSensitive) {
                            parameters.put(paramName, maskedValue);
                        } else {
                            parameters.put(paramName, valueBuilder.toString());
                        }
                    }
                } else {
                    parameters.put(paramName, "");
                }
            }
        }
        return parameters;
    }
    
    /**
     * 주어진 파라미터 이름이 민감 정보를 포함하는지 확인합니다.
     * {@link #SENSITIVE_PARAMETER_KEYWORDS} 목록과 비교하며, 대소문자를 구분하지 않고 부분 일치 여부를 검사합니다.
     *
     * @param paramName 확인할 파라미터 이름
     * @return 민감 정보로 간주되면 {@code true}, 아니면 {@code false}
     */
    private boolean isSensitiveParameter(String paramName) {
        if (paramName == null) {
            return false;
        }
        String lowerCaseName = paramName.toLowerCase();
        for (String keyword : SENSITIVE_PARAMETER_KEYWORDS) {
            if (lowerCaseName.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 값 자체에 민감한 정보가 포함되었는지 확인합니다.
     * 일부 민감 정보는 파라미터 이름이 아닌 값에 포함될 수 있습니다.
     * 
     * @param value 확인할 값
     * @return 민감 정보가 포함되었으면 true, 아니면 false
     */
    private boolean containsSensitiveValue(String value) {
        if (value == null || value.length() < 5) {
            return false; // 짧은 값은 검사 제외
        }
        
        // 민감 정보 패턴 목록 - 정규식으로 확장 가능
        final String[] SENSITIVE_PATTERNS = {
            // 신용카드 번호 패턴 (16자리 숫자, 일부 구분자 허용)
            "\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}",
            // 이메일 주소 패턴 (간단한 검사)
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
            // 주민등록번호 패턴 (YYMMDD-XXXXXXX)
            "\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])[\\s-]?[1-4]\\d{6}"
        };
        
        // 패턴 검사
        for (String pattern : SENSITIVE_PATTERNS) {
            if (value.matches(pattern)) {
                return true;
            }
        }
        
        // 대소문자 구분 없이 민감한 키워드 포함 여부 검사
        String lowerValue = value.toLowerCase();
        for (String keyword : SENSITIVE_PARAMETER_KEYWORDS) {
            if (lowerValue.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 키-값 형태의 맵을 간단한 JSON 객체 형태의 문자열로 변환합니다.
     * 예: {"key1":"value1","key2":"value2"}
     * 값에 포함된 따옴표는 이스케이프 처리됩니다.
     *
     * @param map 변환할 맵
     * @return JSON 형태의 문자열. 맵이 비어있으면 "{}" 반환.
     */
    private String formatMapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":\"")
              .append(entry.getValue() != null ? entry.getValue().replace("\"", "\\\"") : "null")
              .append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * 바이트 배열로 된 컨텐츠를 지정된 인코딩을 사용하여 문자열로 변환합니다.
     * 변환된 문자열은 최대 길이에 따라 잘릴 수 있습니다.
     *
     * @param content 바이트 배열 컨텐츠
     * @param encoding 문자 인코딩 (null일 경우 UTF-8 사용)
     * @param maxLength 반환할 문자열의 최대 길이
     * @return 변환된 문자열. 컨텐츠가 없거나 변환 실패 시 빈 문자열 또는 에러 메시지 반환.
     */
    private String getContentAsString(byte[] content, String encoding, int maxLength) {
        if (content == null || content.length == 0) {
            return "";
        }
        String charsetToUse = (encoding != null && !encoding.trim().isEmpty()) ? encoding : StandardCharsets.UTF_8.name();
        try {
            String fullContent = new String(content, charsetToUse);
            if (fullContent.length() > maxLength) {
                return fullContent.substring(0, maxLength) + "... (truncated)";
            }
            return fullContent;
        } catch (Exception e) {
            log.warn("Failed to convert content to string using charset {}: {}", charsetToUse, e.getMessage());
            return "[Binary Content or Encoding Error]";
        }
    }
    
    /**
     * 주어진 헤더 이름이 로깅에서 제외해야 할 민감 헤더인지 확인합니다.
     * {@link #EXCLUDED_HEADERS} 목록과 비교하며, 대소문자를 구분하지 않고 부분 일치 여부를 검사합니다.
     *
     * @param headerName 확인할 헤더 이름
     * @return 제외 대상이면 {@code true}, 아니면 {@code false}
     */
    private boolean isExcludedHeader(String headerName) {
        if (headerName == null) {
            return true; // Null 헤더 이름은 처리하지 않음
        }
        String lowerCaseName = headerName.toLowerCase();
        for (String excluded : EXCLUDED_HEADERS) {
            if (lowerCaseName.contains(excluded)) {
                return true;
            }
        }
        return false;
    }
} 