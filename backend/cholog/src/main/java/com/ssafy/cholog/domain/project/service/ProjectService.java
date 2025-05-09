package com.ssafy.cholog.domain.project.service;

import com.ssafy.cholog.domain.project.dto.item.UserProjectItem;
import com.ssafy.cholog.domain.project.dto.request.CreateProjectRequest;
import com.ssafy.cholog.domain.project.dto.request.JoinProjectRequest;
import com.ssafy.cholog.domain.project.dto.response.CreateTokenResponse;
import com.ssafy.cholog.domain.project.dto.response.UserProjectListResponse;
import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.project.entity.ProjectUser;
import com.ssafy.cholog.domain.project.repository.ProjectRepository;
import com.ssafy.cholog.domain.project.repository.ProjectUserRepository;
import com.ssafy.cholog.domain.user.entity.User;
import com.ssafy.cholog.domain.user.repository.UserRepository;
import com.ssafy.cholog.global.exception.CustomException;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectUserRepository projectUserRepository;

    public UserProjectListResponse getUserProjectList(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId", userId));

        List<UserProjectItem> projectItemList = projectUserRepository.findByUserOrderByProjectCreatedAtDesc(user).stream()
                .map(UserProjectItem::of)
                .collect(Collectors.toList());

        return UserProjectListResponse.builder()
                .projects(projectItemList)
                .build();
    }

    @Transactional
    public Void createProject(Integer userId, CreateProjectRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = Project.builder()
                .name(request.getName())
                .projectToken(request.getToken())
                .build();
        projectRepository.save(project);

        ProjectUser projectUser = ProjectUser.builder()
                .user(user)
                .project(project)
                .isCreator(true)
                .build();
        projectUserRepository.save(projectUser);

        return null;
    }

    @Transactional
    public CreateTokenResponse createToken(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        // UUID 생성
        String token = UUID.randomUUID().toString();

        // 생성된 토큰을 응답 DTO에 담아 반환
        return CreateTokenResponse.builder()
                .token(token)
                .build();
    }

//    User user2 = userRepository.findById(userId)
//            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND)
//                    .addParameter("userId", userId)
//                    .addParameter("userId", userId));
//    User user3 = userRepository.findById(userId)
//            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "user가 없어요"));


    @Transactional
    public Void joinProject(Integer userId, JoinProjectRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findByProjectToken(request.getToken())
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "토큰이 맞는지 다시 확인해보세요."));

        // 프로젝트 참여 여부 조회
        if (projectUserRepository.existsByProjectAndUser(project, user)) {
            throw new CustomException(ErrorCode.PROJECT_ALREADY_JOINED)
                    .addParameter("userId", userId)
                    .addParameter("projectId", project.getId());
        }

        ProjectUser projectUser = ProjectUser.builder()
                .project(project)
                .user(user)
                .isCreator(false)
                .build();
        projectUserRepository.save(projectUser);

        return null;
    }
}
