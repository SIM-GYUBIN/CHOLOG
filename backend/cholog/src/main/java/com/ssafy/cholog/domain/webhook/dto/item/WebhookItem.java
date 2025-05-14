package com.ssafy.cholog.domain.webhook.dto.item;

import com.ssafy.cholog.domain.webhook.entity.Webhook;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebhookItem {

    @Schema(example = "1")
    private final Integer id;
    @Schema(example = "https://example.com")
    private final String mmURL;
    @Schema(example = "Timeout,Unauthorized,Not_found")
    private final String keywords;
    @Schema(example = "prod")
    private final String notificationENV;
    @Schema(example = "true")
    private final Boolean isEnabled;

    public static WebhookItem of(Webhook webhook){
        return WebhookItem.builder()
                .id(webhook.getId())
                .mmURL(webhook.getMmURL())
                .keywords(webhook.getKeywords())
                .notificationENV(webhook.getNotificationENV())
                .isEnabled(webhook.getIsEnabled())
                .build();
    }
}
