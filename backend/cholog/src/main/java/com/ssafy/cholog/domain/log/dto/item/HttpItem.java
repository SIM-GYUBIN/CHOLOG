package com.ssafy.cholog.domain.log.dto.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpItem {

    private HttpRequestInfo request;
    private HttpResponseInfo response;
    @Schema(example = "200")
    private Long durationMs;

    @Getter
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HttpRequestInfo {
        @Schema(example = "POST")
        private String method;
        @Schema(example = "https://example.com/api/v1/resource")
        private String url;
    }

    @Getter
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HttpResponseInfo {
        @Schema(example = "200")
        private Integer statusCode;
    }
}
