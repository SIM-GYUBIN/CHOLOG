package com.ssafy.cholog.domain.jira.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JiraUserResponse {
    @Schema(example = "true")
    private final boolean exists;
    @Schema(example = "123@example.com")
    private final String userName;
    @Schema(example = "LSldfmvewladok13eldspdsge-fgqlvnewwo")
    private final String jiraToken;
}
