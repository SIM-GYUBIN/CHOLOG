package com.ssafy.cholog.domain.log.service;


import com.ssafy.cholog.domain.log.dto.request.archive.LogArchiveRequest;
import com.ssafy.cholog.domain.log.entity.LogArchive;
import com.ssafy.cholog.domain.log.entity.LogDocument;
import com.ssafy.cholog.domain.log.repository.LogArchiveRepository;
import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.project.repository.ProjectRepository;
import com.ssafy.cholog.domain.project.repository.ProjectUserRepository;
import com.ssafy.cholog.domain.user.entity.User;
import com.ssafy.cholog.domain.user.repository.UserRepository;
import com.ssafy.cholog.global.exception.CustomException;
import com.ssafy.cholog.global.exception.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogArchiveService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectUserRepository projectUserRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final LogArchiveRepository logArchiveRepository;

    @Transactional
    public void archiveLog(Integer userId, Integer projectId, LogArchiveRequest logArchiveRequest) {
        if (!projectUserRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new CustomException(ErrorCode.PROJECT_USER_NOT_FOUND)
                    .addParameter("userId", userId)
                    .addParameter("projectId", projectId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND)
                        .addParameter("userId", userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND)
                        .addParameter("projectId", projectId));

        String projectToken = project.getProjectToken();
        String indexName = "pjt-" + projectToken;

        String logId = logArchiveRequest.getLogId();
        LogDocument logDocument = elasticsearchOperations.get(logId, LogDocument.class, IndexCoordinates.of(indexName));
        if (logDocument == null) {
            throw new CustomException(ErrorCode.LOG_NOT_FOUND)
                    .addParameter("projectId", projectId)
                    .addParameter("logId", logId);
        }

        LogArchive logArchive = LogArchive.builder()
                .project(project)
                .user(user)
                .logId(logId)
                .memo(logArchiveRequest.getArchiveReason())
                .logLevel(logDocument.getLevel())
                .logSource(logDocument.getSource())
                .logType(logDocument.getLogType())
                .logEnvironment(logDocument.getEnvironment())
                .logMessage(logDocument.getMessage())
                .logTimestamp(logDocument.getTimestampOriginal())
                .build();

        logArchiveRepository.save(logArchive);
    }
}
