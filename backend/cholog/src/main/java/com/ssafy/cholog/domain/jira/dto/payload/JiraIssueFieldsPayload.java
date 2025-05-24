package com.ssafy.cholog.domain.jira.dto.payload;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JiraIssueFieldsPayload {

    @JsonProperty("project")
    private JiraIssueIdentifierPayload project;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("description")
    private Object description;

    @JsonProperty("issuetype")
    private JiraIssueIdentifierPayload issuetype;

    @JsonProperty("reporter")
    private JiraIssueIdentifierPayload reporter;

    @JsonProperty("assignee")
    private JiraIssueIdentifierPayload assignee;

    @JsonProperty("labels")
    private List<String> labels;

    @Builder.Default
    private Map<String, Object> additionalFields = new HashMap<>();

    public JiraIssueFieldsPayload addCustomField(String fieldName, Object value) {
        this.additionalFields.put(fieldName, value);
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalFields() {
        return additionalFields;
    }

}