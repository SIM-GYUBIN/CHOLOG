package com.ssafy.cholog.domain.jira.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class JiraUserRequest {
    @NotBlank(message = "JIRA 이메일을 입력해주세요.")
    private String userName;
    @NotBlank(message = "JIRA 토큰을 입력해주세요.")
    private String jiraToken;
}
