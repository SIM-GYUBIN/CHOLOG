package com.cholog.logger.config; // 패키지 경로는 실제 위치에 맞게 조정하세요.

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CHO:LOG SDK를 위해 CORS 설정을 커스터마이징하는 BeanPostProcessor 입니다.
 * UrlBasedCorsConfigurationSource 빈을 찾아 X-Request-Id 헤더를
 * 허용된 헤더(allowed) 및 노출된 헤더(exposed)에 추가합니다.
 */
public class CorsConfigurationCustomizingBeanPostProcessor implements BeanPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(CorsConfigurationCustomizingBeanPostProcessor.class);
    private static final String X_REQUEST_ID_HEADER = "X-Request-Id";
    private static final String X_REQUEST_ID_HEADER_LOWERCASE = "x-request-id";

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof UrlBasedCorsConfigurationSource) {
            log.info("CHO:LOG - Customizing UrlBasedCorsConfigurationSource bean '{}' to ensure '{}' and '{}' headers are handled for CORS.",
                    beanName, X_REQUEST_ID_HEADER, X_REQUEST_ID_HEADER_LOWERCASE);
            UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) bean;

            // UrlBasedCorsConfigurationSource 내부의 CorsConfiguration 맵을 가져와 수정합니다.
            // getCorsConfigurations()가 반환하는 맵의 CorsConfiguration 객체를 직접 수정하면 적용됩니다.
            source.getCorsConfigurations().forEach((pattern, config) -> {
                log.debug("CHO:LOG - Applying X-Request-Id to CORS configuration for pattern: {}", pattern);
                ensureHeaderInAllowed(config);
                ensureHeaderInExposed(config);
            });
        }
        return bean;
    }

    private void ensureHeaderInAllowed(CorsConfiguration config) {
        List<String> allowedHeaders = config.getAllowedHeaders();

        if (allowedHeaders == null) {
            config.setAllowedHeaders(Arrays.asList(X_REQUEST_ID_HEADER, X_REQUEST_ID_HEADER_LOWERCASE));
            log.debug("CHO:LOG - Initialized allowed headers with '{}' and '{}' for a CorsConfiguration.",
                    X_REQUEST_ID_HEADER, X_REQUEST_ID_HEADER_LOWERCASE);
        } else if (!allowedHeaders.contains(CorsConfiguration.ALL)) {
            // 이미 설정된 허용 헤더 목록이 있고, 전체 허용("*")이 아닌 경우
            boolean hasUpperCase = allowedHeaders.stream()
                    .anyMatch(header -> header.equalsIgnoreCase(X_REQUEST_ID_HEADER));
            boolean hasLowerCase = allowedHeaders.stream()
                    .anyMatch(header -> header.equalsIgnoreCase(X_REQUEST_ID_HEADER_LOWERCASE));

            if (!hasUpperCase) {
                config.addAllowedHeader(X_REQUEST_ID_HEADER);
                log.debug("CHO:LOG - Added '{}' to existing allowed headers for a CorsConfiguration.", X_REQUEST_ID_HEADER);
            }

            if (!hasLowerCase) {
                config.addAllowedHeader(X_REQUEST_ID_HEADER_LOWERCASE);
                log.debug("CHO:LOG - Added '{}' to existing allowed headers for a CorsConfiguration.", X_REQUEST_ID_HEADER_LOWERCASE);
            }
        } else {
            // 이미 "*" (모든 헤더 허용)으로 설정된 경우 X-Request-Id는 자동으로 포함됩니다.
            log.debug("CHO:LOG - Allowed headers is already '*' for a CorsConfiguration, '{}' is covered.", X_REQUEST_ID_HEADER);
        }
    }

    private void ensureHeaderInExposed(CorsConfiguration config) {
        List<String> exposedHeaders = config.getExposedHeaders();
        if (exposedHeaders == null) {
            config.setExposedHeaders(Arrays.asList(X_REQUEST_ID_HEADER, X_REQUEST_ID_HEADER_LOWERCASE));
            log.debug("CHO:LOG - Initialized exposed headers with '{}' for a CorsConfiguration.", X_REQUEST_ID_HEADER);
        } else {
            boolean hasUpperCase = exposedHeaders.stream()
                    .anyMatch(header -> header.equalsIgnoreCase(X_REQUEST_ID_HEADER));
            boolean hasLowerCase = exposedHeaders.stream()
                    .anyMatch(header -> header.equalsIgnoreCase(X_REQUEST_ID_HEADER_LOWERCASE));

            if (!hasUpperCase) {
                config.addExposedHeader(X_REQUEST_ID_HEADER);
                log.debug("CHO:LOG - Added '{}' to existing exposed headers for a CorsConfiguration.", X_REQUEST_ID_HEADER);
            }

            if (!hasLowerCase) { // 수정: 대문자 존재 여부와 상관없이 소문자 형식 체크
                config.addExposedHeader(X_REQUEST_ID_HEADER_LOWERCASE);
                log.debug("CHO:LOG - Added '{}' to existing exposed headers for a CorsConfiguration.", X_REQUEST_ID_HEADER_LOWERCASE);
            }
        }
    }
}
