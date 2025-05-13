package com.ssafy.cholog.domain.log.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LogTimelineResponse {
    private LocalDateTime timestamp;
    private Integer logCount;
}
