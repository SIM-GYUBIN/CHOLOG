package com.ssafy.cholog.domain.webhook.dto.mattermost;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    private String fallback;
    private String color;
    private String title;
    @JsonProperty("title_link")
    private String titleLink;
    private List<Field> fields;
}
