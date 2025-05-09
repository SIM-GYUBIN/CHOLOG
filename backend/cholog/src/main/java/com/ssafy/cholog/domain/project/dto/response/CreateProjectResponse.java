package com.ssafy.cholog.domain.project.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateProjectResponse {
    private final String token;
}
