package com.ssafy.cholog.domain.jira.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JiraIssueCreationResponse {
    private String issueKey;
    private String issueUrl;
}