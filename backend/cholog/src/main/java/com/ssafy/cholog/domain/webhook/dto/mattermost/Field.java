package com.ssafy.cholog.domain.webhook.dto.mattermost;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Field {
    @JsonProperty("short")
    private boolean isShort;
    private String title;
    private String value;
}
