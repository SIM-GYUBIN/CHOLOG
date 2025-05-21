package com.ssafy.cholog.domain.project.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecreateTokenResponse {
    private final String token;
}
