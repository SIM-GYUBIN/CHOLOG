package com.ssafy.cholog.domain.project.dto.item;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProjectItem {
    private final Integer id;
    private final String name;
}
