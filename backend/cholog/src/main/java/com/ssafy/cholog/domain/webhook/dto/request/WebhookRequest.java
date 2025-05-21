package com.ssafy.cholog.domain.webhook.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebhookRequest {

    @NotBlank(message = "프로젝트 웹훅 URL을 입력해주세요.")
    private String mmURL;

    private String keywords;
    private String notificationENV;
    private Boolean isEnabled;

}
