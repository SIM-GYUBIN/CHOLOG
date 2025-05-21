package com.ssafy.cholog.domain.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateProjectRequest {

    @NotBlank(message = "프로젝트 이름을 입력해주세요.")
    private String name;

}
