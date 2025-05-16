package com.ssafy.cholog.domain.log.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // ES에는 있지만 Document에 정의되지 않은 필드는 무시
public abstract class BaseLogDocument {

    @Id
    private String id;

    @Field(name = "@timestamp", type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'", timezone = "UTC")
    @Schema(description = "Elasticsearch 표준 타임스탬프 (UTC)", example = "2025-05-16T12:30:45.123456789Z")
    private Instant timestampEs; // ES 표준 타임스탬프

    @Field(name = "sequence", type = FieldType.Long)
    @Schema(description = "로그 순서 번호 (동일 타임스탬프 내 순서 보장용)")
    private Long sequence;

    @Field(name = "level", type = FieldType.Keyword)
    @Schema(description = "로그 레벨", example = "INFO")
    private String level;

    @Field(name = "message", type = FieldType.Text) // 필요시 .keyword 멀티 필드 고려
    @Schema(description = "로그 메시지 본문")
    private String message;

    @Field(name = "source", type = FieldType.Keyword)
    @Schema(description = "로그 발생 소스", example = "frontend")
    private String source;

    @Field(name = "projectKey", type = FieldType.Keyword)
    @Schema(description = "프로젝트 식별 키", example = "my-awesome-project")
    private String projectKey;

    @Field(name = "environment", type = FieldType.Keyword)
    @Schema(description = "배포 환경 (예: development, staging, production)", example = "production")
    private String environment;

    @Field(name = "traceId", type = FieldType.Keyword)
    @Schema(description = "분산 추적 ID", example = "abc-123-def-456")
    private String traceId;

    @Field(name = "logger", type = FieldType.Keyword)
    @Schema(description = "로거 이름", example = "chologger")
    private String logger;

    @Field(name = "logType", type = FieldType.Keyword)
    @Schema(description = "로그 타입 (예: FRONTEND, BACKEND)", example = "FRONTEND")
    private String logType; // 각 구체 클래스에서 설정
}