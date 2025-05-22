package com.ssafy.cholog.domain.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JoinProjectRequest {
    @NotBlank(message = "프로젝트 토큰을 입력해주세요.")
    private String token;
}
