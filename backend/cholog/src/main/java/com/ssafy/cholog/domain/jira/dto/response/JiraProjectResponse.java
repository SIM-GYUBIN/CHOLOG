package com.ssafy.cholog.domain.jira.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JiraProjectResponse {
    @Schema(example = "true")
    private final boolean exists;
    @Schema(example = "https://ssafy.atlassian.net/")
    private final String instanceUrl;
    @Schema(example = "project-D13l0")
    private final String projectKey;
}
