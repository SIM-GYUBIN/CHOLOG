package com.ssafy.cholog.domain.jira.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class JiraIssueRequest {

    @NotBlank(message = "이슈 요약(summary)은 필수입니다.")
    private String summary;

    private String description;

    @NotBlank(message = "이슈 유형(issueType)은 필수입니다.")
    private String issueType; // 예: "Bug", "Task", "Story" (Jira에 정의된 이슈 유형 이름)
    @NotBlank(message = "보고자(reporter)는 필수입니다.")
    private String reporterName;

    private String assigneeName;
}