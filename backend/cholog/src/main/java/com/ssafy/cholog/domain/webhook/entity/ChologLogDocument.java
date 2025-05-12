package com.ssafy.cholog.domain.webhook.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor // JPA/Elasticsearch 등에서 객체 생성 시 필요할 수 있음
@AllArgsConstructor // 모든 필드를 받는 생성자
@Builder // 빌더 패턴 사용 가능
@ToString // 디버깅 시 객체 내용 확인 용이
@Document(indexName = "cholog-logs-*") // Elasticsearch 인덱스 이름 또는 패턴
public class ChologLogDocument {

    @Id // Elasticsearch 문서의 _id 필드와 매핑
    private String id;

    @Field(type = FieldType.Keyword) // 검색 및 집계에 사용될 프로젝트 식별자 (보통 API Key)
    private String projectId;

    @Field(type = FieldType.Keyword) // 로그 레벨 (ERROR, WARN, INFO 등)
    private String level;

    @Field(type = FieldType.Text, analyzer = "standard") // 전문 검색이 가능한 메시지 본문
    private String message;

    // Elasticsearch의 @timestamp 필드와 매핑
    // UTC 기준으로 저장하고 조회하는 것이 좋음
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSX")
    // 'uuuu'는 연도, 'X'는 ISO 8601 시간대 지정자 (+00, Z 등). SSS는 밀리초.
    // Elasticsearch 기본 date format 중 하나인 strict_date_optional_time 또는 epoch_millis 등과 호환되도록 설정.
    private LocalDateTime timestamp;

    @Field(type = FieldType.Keyword) // 애플리케이션의 실행 환경 (예: "production", "development")
    private String appEnvironment;

    @Field(type = FieldType.Keyword) // 애플리케이션의 이름
    private String appName;

    @Field(type = FieldType.Text) // 스택 트레이스 (길이가 길 수 있음)
    private String stackTrace;

    @Field(type = FieldType.Keyword) // 분산 추적 또는 요청 흐름을 위한 Trace ID
    private String traceId;

    // Elasticsearch 인덱스 이름 (_index 메타 필드 값)
    // 이 필드는 보통 Elasticsearch 검색 결과의 SearchHit 객체에서 직접 얻어오므로,
    // 문서 모델 자체에 필수로 포함하지 않아도 됩니다.
    // 필요하다면 검색 후 채워 넣거나, @ReadOnlyProperty, @Transient 등으로 표시할 수 있습니다.
    // 여기서는 Mattermost 메시지에 사용하기 위해 필드로 추가합니다.
    // (주의: SDK에서 이 값을 직접 설정하여 보내는 것은 일반적이지 않음)
    @Field(type = FieldType.Keyword, index = false, docValues = false) // 저장만 하고 검색/집계에는 사용 안 함
    private String esIndex;

}