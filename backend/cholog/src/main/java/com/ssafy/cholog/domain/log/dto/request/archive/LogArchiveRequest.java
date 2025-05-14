package com.ssafy.cholog.domain.log.dto.request.archive;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LogArchiveRequest {
    private String logId;
    private String archiveReason;
}
