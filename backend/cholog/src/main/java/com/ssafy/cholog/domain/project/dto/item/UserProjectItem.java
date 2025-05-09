package com.ssafy.cholog.domain.project.dto.item;

import com.ssafy.cholog.domain.project.entity.ProjectUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserProjectItem {
    @Schema(example = "3")
    private final Integer id;
    @Schema(example = "와인 한 사발 프로젝트")
    private final String name;
    @Schema(example = "143258dkfjwoi100mfvl")
    private final String projectToken;
    @Schema(example = "true")
    private final Boolean isCreator;
    @Schema(example = "2025-05-08 16:20:06.459154")
    private final LocalDateTime createdAt;

    public static UserProjectItem of(ProjectUser projectUser) {
        return UserProjectItem.builder()
                .id(projectUser.getProject().getId())
                .name(projectUser.getProject().getName())
                .projectToken(projectUser.getProject().getProjectToken())
                .isCreator(projectUser.getIsCreator())
                .createdAt(projectUser.getProject().getCreatedAt())
                .build();
    }
}