package com.ssafy.cholog.domain.jira.dto.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JiraIssueCreatePayload {

    @JsonProperty("fields")
    private JiraIssueFieldsPayload fields;

}