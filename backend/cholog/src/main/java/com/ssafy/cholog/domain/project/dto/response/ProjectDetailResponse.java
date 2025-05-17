package com.ssafy.cholog.domain.project.dto.response;

import com.ssafy.cholog.domain.project.entity.Project;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectDetailResponse {
    @Schema(example = "와인 한 사발 프로젝트")
    private final String name;
    @Schema(example = "143258dkfjwoi100mfvl")
    private final String projectToken;

    public static ProjectDetailResponse of(Project project) {
        return ProjectDetailResponse.builder()
                .name(project.getName())
                .projectToken(project.getProjectToken())
                .build();
    }
}