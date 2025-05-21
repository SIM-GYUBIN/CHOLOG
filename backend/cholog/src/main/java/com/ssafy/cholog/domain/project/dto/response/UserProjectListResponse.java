package com.ssafy.cholog.domain.project.dto.response;

import com.ssafy.cholog.domain.project.dto.item.UserProjectItem;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserProjectListResponse {
    private final List<UserProjectItem> projects;
}
