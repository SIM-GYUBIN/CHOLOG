package com.ssafy.cholog.domain.report.controller;

import com.microsoft.playwright.*;
import com.ssafy.cholog.domain.report.dto.request.PdfReportRequest;
import com.ssafy.cholog.domain.report.dto.request.ReportRequest;
import com.ssafy.cholog.domain.report.dto.response.ReportResponse;
import com.ssafy.cholog.domain.report.service.ReportService;
import com.ssafy.cholog.global.aop.swagger.ApiErrorCodeExamples;
import com.ssafy.cholog.global.common.response.CommonResponse;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import com.ssafy.cholog.global.security.auth.UserPrincipal;
import com.ssafy.cholog.global.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@Tag(name = "로그 리포트", description = "로그 리포트 API")
public class ReportController {

    private final ReportService reportService;
    private final AuthenticationUtil authenticationUtil;

    @PostMapping("/{projectId}")
    @Operation(summary = "로그 리포트 발급", description = "로그 리포트 발급 API")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.PROJECT_USER_NOT_FOUND, ErrorCode.PROJECT_NOT_FOUND, ErrorCode.LOG_START_TIME_AFTER_END_TIME, ErrorCode.INTERNAL_SERVER_ERROR})
    public ResponseEntity<CommonResponse<ReportResponse>> archiveLog(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer projectId,
            @RequestBody ReportRequest reportRequest
    ) {
        Integer userId = authenticationUtil.getCurrentUserId(userPrincipal);

        return CommonResponse.ok(reportService.makeReport(userId, projectId, reportRequest));
    }

    @PostMapping(value = "/{projectId}/pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "로그 리포트 PDF 다운로드", description = "제공된 HTML 콘텐츠를 기반으로 로그 리포트 PDF를 생성하여 다운로드합니다.")
    @PreAuthorize("isAuthenticated()")
    @ApiErrorCodeExamples({ErrorCode.PROJECT_NOT_FOUND, ErrorCode.INTERNAL_SERVER_ERROR})
    public ResponseEntity<byte[]> downloadReportAsPdf(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer projectId,
            @RequestBody PdfReportRequest request
    ) {
        try {
            byte[] pdfBytes = reportService.generatePdfFromHtml(request.getHtmlContent(), projectId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF); // 응답 타입을 PDF로 설정

            // 동적 파일명 생성 로직 (예시)
            String startDateStr = (request.getStartDate() != null && !request.getStartDate().isEmpty())
                    ? request.getStartDate()
                    : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String endDateStr = (request.getEndDate() != null && !request.getEndDate().isEmpty())
                    ? request.getEndDate()
                    : "";
            String periodSuffix = endDateStr.isEmpty() ? startDateStr : startDateStr + "_to_" + endDateStr;
            // 프로젝트 이름 등을 추가하여 더 의미있는 파일명 생성 가능
            String filename = String.format("Project-%d_Report_%s.pdf", projectId, periodSuffix)
                    .replace(":", "-").replace(" ", "_"); // 파일명에 부적합한 문자 처리

            headers.setContentDispositionFormData(filename, filename); // 파일 다운로드 및 파일명 지정
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0"); // 캐시 제어

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (PlaywrightException e) {
            String errorMessage = "PDF 생성 중 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요. (오류: Playwright)";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN) // 오류 메시지 타입
                    .body(errorMessage.getBytes());
        } catch (IllegalArgumentException e) {
            String errorMessage = "PDF 생성 요청 오류: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(errorMessage.getBytes());
        } catch (Exception e) {
            String errorMessage = "PDF 생성 중 알 수 없는 오류가 발생했습니다.";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(errorMessage.getBytes());
        }
    }

}
