package com.ssafy.cholog.domain.project.service;

import com.ssafy.cholog.domain.log.service.LogService;
import com.ssafy.cholog.domain.project.dto.item.UserProjectItem;
import com.ssafy.cholog.domain.project.dto.request.CreateProjectRequest;
import com.ssafy.cholog.domain.project.dto.request.JoinProjectRequest;
import com.ssafy.cholog.domain.project.dto.request.RecreateTokenRequest;
import com.ssafy.cholog.domain.project.dto.request.UpdateProjectNameRequest;
import com.ssafy.cholog.domain.project.dto.response.CreateProjectResponse;
import com.ssafy.cholog.domain.project.dto.response.ProjectDetailResponse;
import com.ssafy.cholog.domain.project.dto.response.RecreateTokenResponse;
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
    private final LogService logService;

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
    public CreateProjectResponse createProject(Integer userId, CreateProjectRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        String token = createToken();

        Project project = Project.builder()
                .name(request.getName())
                .projectToken(token)
                .build();
        projectRepository.save(project);

        ProjectUser projectUser = ProjectUser.builder()
                .user(user)
                .project(project)
                .isCreator(true)
                .build();
        projectUserRepository.save(projectUser);

        logService.createIndex(token);

        return CreateProjectResponse.builder().token(token).build();
    }

    public String createToken() {
        return UUID.randomUUID().toString();
    }

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

    @Transactional
    public RecreateTokenResponse recreateToken(Integer userId, RecreateTokenRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId",request.getProjectId()));

        ProjectUser projectUser = projectUserRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_PROJECT_USER)
                                .addParameter("userId", userId)
                                .addParameter("projectId", project.getId()));

        // 해당 유저가 프로젝트 생성자인지 확인
        if (!projectUser.getIsCreator()) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS, "토큰 재발급 권한이 없습니다.");
        }

        String token = createToken();
        project.updateProjectToken(token);

        return RecreateTokenResponse.builder()
                .token(token)
                .build();
    }

    @Transactional
    public Void updateProjectName(Integer userId, Integer projectId, UpdateProjectNameRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId",projectId));

        ProjectUser projectUser = projectUserRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_PROJECT_USER)
                        .addParameter("userId", userId)
                        .addParameter("projectId", project.getId()));

        // 해당 유저가 프로젝트 생성자인지 확인
        if (!projectUser.getIsCreator()) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS, "프로젝트 수정 권한이 없습니다.");
        }

        project.updateProjectName(request.getName());

        return null;
    }

    @Transactional
    public Void deleteProject(Integer userId, Integer projectId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId",projectId));

        ProjectUser projectUser = projectUserRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_PROJECT_USER)
                        .addParameter("userId", userId)
                        .addParameter("projectId", project.getId()));

        // 해당 유저가 프로젝트 생성자인지 확인
        if (!projectUser.getIsCreator()) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS, "프로젝트 삭제 권한이 없습니다.");
        }

        projectRepository.delete(project);

        return null;
    }

    public ProjectDetailResponse getProjectDetail(Integer userId, Integer projectId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId",projectId));

        ProjectUser projectUser = projectUserRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_PROJECT_USER)
                        .addParameter("userId", userId)
                        .addParameter("projectId", project.getId()));

        return ProjectDetailResponse.of(project);
    }

    @Transactional
    public Void withdrawProject(Integer userId, Integer projectId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "userId",userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND, "projectId",projectId));

        ProjectUser projectUser = projectUserRepository.findByUserAndProject(user, project)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_PROJECT_USER)
                        .addParameter("userId", userId)
                        .addParameter("projectId", project.getId()));

        // 해당 유저가 프로젝트 생성자인지 확인 -> 생성자는 탈퇴 불가능
        if (projectUser.getIsCreator()) {
            throw new CustomException(ErrorCode.CREATOR_CANNOT_WITHDRAW);
        }

        projectUserRepository.delete(projectUser);

        return null;
    }
}
