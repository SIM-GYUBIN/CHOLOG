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
    private Object description; // 일반 텍스트 또는 Atlassian Document Format(ADF) 객체

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

    // 사용자 정의 필드 및 기타 동적 필드를 추가하는 편의 메소드
    // 이 메소드는 빌더 패턴과 함께 사용하기보다는, 객체 생성 후 추가할 때 유용
    // 빌더에 customField(key, value) 같은 메소드를 정의하는 것도 방법임.
    public JiraIssueFieldsPayload addCustomField(String fieldName, Object value) {
        this.additionalFields.put(fieldName, value);
        return this; // 체이닝을 위해
    }

    // 이 메소드는 Lombok @Getter와 별개로 @JsonAnyGetter를 위해 필요
    @JsonAnyGetter
    public Map<String, Object> getAdditionalFields() {
        return additionalFields;
    }

}