package com.ssafy.cholog.domain.jira.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class JiraIssueRequest {
    @NotBlank(message = "이슈 요약을 입력해주세요.")
    private String summary; // 이슈 제목

    private String description; // 이슈 설명 (HTML 또는 Jira 위키 마크업 가능)

    @NotBlank(message = "이슈 유형을 입력해주세요.")
    private String issueType; // 예: "Bug", "Task", "Story" (Jira에 설정된 이슈 유형 이름)

    private String assigneeName; // 담당자 Jira 사용자 이름 (선택 사항)
    private String reporterName; // 보고자 Jira 사용자 이름 (선택 사항, 시스템에서 자동으로 설정할 수도 있음)
    private String priorityName; // 우선순위 이름 (선택 사항, 예: "High", "Medium")
    private List<String> labels; // 레이블 (선택 사항)
    private Map<String, Object> customFields; // 사용자 정의 필드 (선택 사항, 예: {"customfield_10010": "Value"})
}
