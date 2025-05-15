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
}