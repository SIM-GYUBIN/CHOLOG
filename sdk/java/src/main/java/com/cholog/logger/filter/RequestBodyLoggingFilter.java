package com.cholog.logger.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HTTP 요청 본문(body)을 로깅하기 위한 서블릿 필터입니다.
 * 이 필터는 {@link HttpServletRequest}의 입력 스트림을 여러 번 읽을 수 있도록
 * {@link ContentCachingRequestWrapper}로 요청을 래핑하는 역할을 주로 수행합니다.
 * 래핑된 요청은 필터 체인을 따라 다음 필터로 전달되며, 여기서 실제 요청 본문을 읽고 MDC에 저장합니다.
 *
 * 이 필터는 {@link RequestTimingFilter} 직후, 그리고 {@link RequestResponseLoggingFilter} 보다는 먼저 실행되어야
 * 요청 본문이 올바르게 캐시되고 로깅될 수 있습니다.
 * 필터 순서는 {@link Order} 어노테이션으로 제어됩니다 ({@code Ordered.HIGHEST_PRECEDENCE + 10}).
 * 로깅은 지정된 HTTP 메소드 및 컨텐츠 타입에 대해서만 수행됩니다.
 *
 * @version 1.8.6
 * @see RequestTimingFilter
 * @see RequestResponseLoggingFilter
 * @see ContentCachingRequestWrapper
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // RequestTimingFilter 바로 다음에 실행되도록 설정
public class RequestBodyLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestBodyLoggingFilter.class);
    /** 로깅할 요청 본문의 최대 길이 (bytes). 이 길이를 초과하면 잘리고 "... (truncated)"가 추가됨. */
    private static final int MAX_PAYLOAD_LENGTH = 10000;

    /**
     * 필터의 주 로직을 실행합니다.
     * HTTP 요청인 경우, 요청 메소드와 컨텐츠 타입을 확인하여 본문 로깅 대상인지 판단합니다.
     * 대상인 경우, 요청을 {@link ContentCachingRequestWrapper}로 래핑하고 필터 체인을 계속 진행합니다.
     * 체인 실행 후 (즉, 요청 처리가 완료된 후), 래핑된 요청에서 본문을 읽어 MDC에 저장하고 디버그 로그를 남깁니다.
     *
     * @param request  서블릿 요청
     * @param response 서블릿 응답
     * @param chain    필터 체인
     * @throws IOException      요청/응답 처리 중 I/O 오류 발생 시
     * @throws ServletException 서블릿 관련 오류 발생 시
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            
            String method = httpRequest.getMethod();
            String contentType = httpRequest.getContentType();
            
            if (shouldLogRequestBody(method, contentType)) {
                // ContentCachingRequestWrapper는 한번만 래핑되어야 합니다.
                // 이미 다른 필터에 의해 래핑된 경우, 해당 인스턴스를 재사용합니다.
                ContentCachingRequestWrapper wrappedRequest = 
                        request instanceof ContentCachingRequestWrapper ?
                                (ContentCachingRequestWrapper) request :
                                new ContentCachingRequestWrapper(httpRequest);
                try {
                    // 래핑된 요청으로 필터 체인 계속 진행
                    // 이 과정에서 컨트롤러 또는 다른 필터가 요청 본문을 읽을 수 있습니다.
                    chain.doFilter(wrappedRequest, response);
                } finally {
                    // 요청 처리가 완료된 후 (성공 또는 예외 발생 무관)
                    // 캐시된 요청 본문을 읽어 로깅합니다.
                    // 여기서 본문을 읽어야 ContentCachingRequestWrapper에 내용이 캐시됩니다.
                    logRequestBody(wrappedRequest);
                }
                return; // 래핑 및 로깅 처리 완료
            }
        }
        
        // 로깅 대상이 아니거나 HTTP 요청이 아닌 경우, 원래 요청/응답 객체로 계속 진행
        chain.doFilter(request, response);
    }
    
    /**
     * 주어진 HTTP 메소드와 컨텐츠 타입을 기반으로 요청 본문을 로깅해야 하는지 여부를 결정합니다.
     * 일반적으로 GET, HEAD, DELETE 등의 메소드는 본문을 포함하지 않으므로 로깅 대상에서 제외합니다.
     * 또한, 컨텐츠 타입이 텍스트 기반(JSON, XML, TEXT, FORM)이 아닌 경우 (예: 바이너리 파일 업로드)도 제외합니다.
     *
     * @param method HTTP 메소드 (예: "GET", "POST")
     * @param contentType 요청의 Content-Type 헤더 값
     * @return 요청 본문을 로깅해야 하면 {@code true}, 그렇지 않으면 {@code false}
     */
    private boolean shouldLogRequestBody(String method, String contentType) {
        if (method == null) return false;
        
        // 본문을 가질 가능성이 낮은 HTTP 메소드들은 제외
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || 
            "DELETE".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method) || 
            "TRACE".equalsIgnoreCase(method)) {
            return false;
        }
        
        // 컨텐츠 타입이 명시되지 않았거나, 로깅하기 적합하지 않은 타입은 제외
        if (contentType == null) {
            return false; // 컨텐츠 타입 없는 경우는 일반적으로 본문도 없음
        }
        
        String lowerCaseContentType = contentType.toLowerCase();
        return lowerCaseContentType.contains("json") || 
               lowerCaseContentType.contains("xml") || 
               lowerCaseContentType.contains("text") || // text/plain 등
               lowerCaseContentType.contains("form"); // application/x-www-form-urlencoded
    }
    
    /**
     * {@link ContentCachingRequestWrapper}에 캐시된 요청 본문을 읽어 MDC에 추가하고 디버그 로그를 남깁니다.
     * 요청 본문의 크기가 {@link #MAX_PAYLOAD_LENGTH}를 초과하면 잘라서 저장합니다.
     *
     * @param request 래핑된 HTTP 요청 객체, 이미 {@code getContentAsByteArray()} 등을 통해 본문이 캐시된 상태여야 함
     */
    private void logRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray(); // 이 시점에 요청 본문이 캐시됨
        if (content.length > 0) {
            String requestBody;
            try {
                // ContentCachingRequestWrapper 내부 버퍼에서 직접 읽어옴
                requestBody = new String(content, request.getCharacterEncoding() != null ? 
                                                  request.getCharacterEncoding() : 
                                                  StandardCharsets.UTF_8.name());
            } catch (IOException e) {
                // 발생하기 어려운 예외 (이미 메모리에 있는 바이트 배열을 문자열로 변환)
                log.warn("Failed to read cached request body content: {}", e.getMessage());
                requestBody = "[Failed to decode cached request body]";
            }
            
            if (requestBody.length() > MAX_PAYLOAD_LENGTH) {
                requestBody = requestBody.substring(0, MAX_PAYLOAD_LENGTH) + "... (truncated)";
            }
            
            MDC.put("request_body", requestBody);
            // 실제 로그 전송은 CentralLogAppender가 MDC 정보를 종합하여 처리하므로,
            // 여기서는 디버그 레벨로 간단히 기록하여 필터 동작 확인용으로 사용합니다.
            log.debug("Request body captured ({} bytes): {}", content.length, requestBody);
        }
    }
} 