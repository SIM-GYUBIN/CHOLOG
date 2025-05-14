package com.ssafy.cholog.domain.jira.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class JiraProjectRequest {
    @NotBlank(message = "JIRA 도메인을 입력해주세요.")
    private String instanceUrl;
    @NotBlank(message = "JIRA 프로젝트키를 입력해주세요.")
    private String projectKey;
}
