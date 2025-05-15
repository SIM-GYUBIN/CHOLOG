package com.ssafy.cholog.domain.report.dto.response;

import com.ssafy.cholog.domain.report.dto.item.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReportResponse {
//    @Schema(example = "e170d545-af0a-413e-b892-697488b3ed28")
//    private String reportId;
    @Schema(example = "1")
    private String projectId;
    @Schema(example = "2025년 05월 01일 ~ 2025년 05월 15일")
    private String periodDescription;
    @Schema(example = "2025-05-15T08:14:10.3379552Z")
    private String generatedAt;

    private TotalLogCounts totalLogCounts;
    private LogLevelDistribution logLevelDistribution;
    private List<TopErrorOccurrence> topErrors;
    private List<SlowApiEndpoint> slowBackendApis;
}
