package com.ssafy.cholog.domain.jira.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class JiraRequest {
    @NotBlank(message = "JIRA 토큰을 입력해주세요.")
    private String jiraToken;
}
