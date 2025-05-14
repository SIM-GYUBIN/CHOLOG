package com.ssafy.cholog.domain.log.entity;

import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.user.entity.User;
import com.ssafy.cholog.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "log_archive")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LogArchive extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "log_id", nullable = false, length = 100)
    private String logId;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "log_level")
    private String logLevel;

    @Column(name = "log_source")
    private String logSource;

    @Column(name = "log_type")
    private String logType;

    @Column(name = "log_environment")
    private String logEnvironment;

    @Column(name = "log_message")
    private String logMessage;

    @Column(name = "log_timestamp")
    private Instant logTimestamp;
}
