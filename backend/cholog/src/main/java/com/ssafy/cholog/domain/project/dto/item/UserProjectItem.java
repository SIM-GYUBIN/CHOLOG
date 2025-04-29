package com.ssafy.cholog.domain.project.dto.item;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProjectItem {
    @Schema(example = "3")
    private final Integer id;
    @Schema(example = "와인 한 사발 프로젝트")
    private final String name;
}
