package com.ssafy.cholog.domain.report.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PdfReportRequest {
    private String htmlContent;
    private String startDate;
    private String endDate;
}
