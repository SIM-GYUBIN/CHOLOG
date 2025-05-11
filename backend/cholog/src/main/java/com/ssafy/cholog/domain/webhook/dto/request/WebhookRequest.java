package com.ssafy.cholog.domain.webhook.dto.request;

import com.ssafy.cholog.domain.webhook.enums.LogLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebhookRequest {

    @NotBlank(message = "프로젝트 웹훅 URL을 입력해주세요.")
    private String mmURL;

    private LogLevel logLevel;
    private String notificationENV;
    private Boolean isEnabled;

}
