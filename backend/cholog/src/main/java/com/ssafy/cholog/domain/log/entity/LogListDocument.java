package com.ssafy.cholog.domain.log.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Data
@Document(indexName = "pjt-*") // 인덱스 패턴 지정, 실제 사용 시 동적으로 정확한 인덱스명 지정
@JsonIgnoreProperties(ignoreUnknown = true) // ES에는 있지만 Document에 정의되지 않은 필드는 무시
public class LogListDocument {
    @Id
    private String id;

    @Field(name = "timestamp", type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS]'Z'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestampOriginal;

    @Field(name = "sequence", type = FieldType.Long)
    private Long sequence;

    @Field(name = "level", type = FieldType.Keyword)
    private String level;

    @Field(name = "message", type = FieldType.Text)
    private String message;

    @Field(name = "source", type = FieldType.Keyword)
    private String source;

    @Field(name = "projectKey", type = FieldType.Keyword)
    private String projectKey;

    @Field(name = "environment", type = FieldType.Keyword)
    private String environment;
}
