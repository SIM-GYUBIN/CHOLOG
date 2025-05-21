package com.ssafy.cholog.domain.jira.dto.item;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JiraUserItem {
    @Schema(example = "123@example.com")
    private final String userName;
}
