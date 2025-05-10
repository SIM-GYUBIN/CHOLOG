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
    @Schema(example = "https://cholog.com")
    private final String mmURL;
    @Schema(example = "sdfsfd-aswtgg300b-dfa")
    private final String jiraToken;
    @Schema(example = "false")
    private final Boolean isCreator;

    public static ProjectDetailResponse of(Project project, Boolean isCreator) {
        return ProjectDetailResponse.builder()
                .name(project.getName())
                .projectToken(project.getProjectToken())
                .mmURL(project.getMmURL())
                .jiraToken(project.getJiraToken())
                .isCreator(isCreator)
                .build();
    }
}