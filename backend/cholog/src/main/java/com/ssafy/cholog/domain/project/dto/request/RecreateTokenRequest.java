package com.ssafy.cholog.domain.project.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecreateTokenRequest {
    @NotNull(message = "프로젝트 Id를 주세요.")
    private Integer projectId;
}
