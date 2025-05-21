package com.ssafy.cholog.domain.webhook.dto.mattermost;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MattermostPayload {
    private String text;
    private Props props;
    private List<Attachment> attachments;
}
