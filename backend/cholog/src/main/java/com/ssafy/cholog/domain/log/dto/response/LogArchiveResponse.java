package com.ssafy.cholog.domain.log.dto.response;

import com.ssafy.cholog.domain.log.entity.LogArchive;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class LogArchiveResponse {
    @Schema(example = "3fisdhjfoi2")
    private String logId;
    @Schema(example = "BE심규빈")
    private String nickname;
    @Schema(example = "나중에 다시볼것")
    private String memo;
    @Schema(example = "ERROR")
    private String logLevel;
    @Schema(example = "frontend")
    private String logSource;
    @Schema(example = "error")
    private String logType;
    @Schema(example = "production")
    private String logEnvironment;
    @Schema(example = "로그 내용")
    private String logMessage;
    @Schema(example = "2023-10-01T12:00:00Z")
    private Instant logTimestamp;

    public static LogArchiveResponse of(LogArchive logArchive) {
        return LogArchiveResponse.builder()
                .logId(logArchive.getLogId())
                .nickname(logArchive.getUser().getNickname())
                .memo(logArchive.getMemo())
                .logLevel(logArchive.getLogLevel())
                .logSource(logArchive.getLogSource())
                .logType(logArchive.getLogType())
                .logEnvironment(logArchive.getLogEnvironment())
                .logMessage(logArchive.getLogMessage())
                .logTimestamp(logArchive.getLogTimestamp())
                .build();
    }
}
