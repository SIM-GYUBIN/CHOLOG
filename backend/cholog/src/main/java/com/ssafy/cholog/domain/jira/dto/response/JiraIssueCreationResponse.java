package com.ssafy.cholog.domain.jira.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JiraIssueCreationResponse {
    private String issueKey;
    private String issueUrl; // 사용자가 Jira에서 이슈를 바로 볼 수 있는 URL
    private String issueApiUrl; // Jira API에서 해당 이슈를 가리키는 URL (응답의 'self')
    private String issueId; // Jira 내부 ID (응답의 'id')
}