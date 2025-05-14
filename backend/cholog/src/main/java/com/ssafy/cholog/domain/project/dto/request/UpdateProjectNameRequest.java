package com.ssafy.cholog.domain.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UpdateProjectNameRequest {
    @NotBlank(message = "프로젝트 이름을 입력해주세요.")
    private String name;
}
