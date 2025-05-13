package com.ssafy.cholog.domain.log.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LogStatsResponse {
    private Long total;
    private Integer trace;
    private Integer debug;
    private Integer info;
    private Integer warn;
    private Integer error;
    private Integer fatal;


    public static LogStatsResponse of(long total, int trace, int debug, int info, int warn, int error, int fatal) {
        return LogStatsResponse.builder()
                .total(total)
                .trace(trace)
                .debug(debug)
                .info(info)
                .warn(warn)
                .error(error)
                .fatal(fatal)
                .build();
    }
}
