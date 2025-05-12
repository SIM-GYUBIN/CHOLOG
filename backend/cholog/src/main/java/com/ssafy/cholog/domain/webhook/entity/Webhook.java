package com.ssafy.cholog.domain.webhook.entity;

import com.ssafy.cholog.domain.project.entity.Project;
import com.ssafy.cholog.domain.webhook.enums.LogLevel;
import com.ssafy.cholog.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Webhook extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @Column(name = "mm_url", nullable = false)
    private String mmURL;

    @Column(name = "notification_env", length = 50)
    private String notificationENV;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", nullable = false, length = 20)
    private LogLevel logLevel;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    @Builder.Default
    @Column(name = "last_checked_timestamp")
    private LocalDateTime lastCheckedTimestamp = LocalDateTime.now();

    public void updateMmURL(String mmURL){ this.mmURL = mmURL; }
    public void updateNotificationENV(String notificationENV){ this.notificationENV = notificationENV; }
    public void updateLogLevel(LogLevel logLevel) { this.logLevel = logLevel; }
    public void updateIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    public void updateLastCheckedTimestamp(LocalDateTime timestamp) { this.lastCheckedTimestamp = timestamp; }
}
