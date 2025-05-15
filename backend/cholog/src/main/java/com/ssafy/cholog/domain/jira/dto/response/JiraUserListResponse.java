package com.ssafy.cholog.domain.jira.dto.response;

import com.ssafy.cholog.domain.jira.dto.item.JiraUserItem;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class JiraUserListResponse {
    private final List<JiraUserItem> userNames;
}
