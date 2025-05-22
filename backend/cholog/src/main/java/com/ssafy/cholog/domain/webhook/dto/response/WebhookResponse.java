package com.ssafy.cholog.domain.webhook.dto.response;

import com.ssafy.cholog.domain.webhook.dto.item.WebhookItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebhookResponse {

    @Schema(example = "true")
    private final boolean exists;

    private final WebhookItem webhookItem;

}
