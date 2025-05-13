package com.ssafy.cholog.domain.webhook.entity;

import com.ssafy.cholog.domain.project.entity.Project;
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

    @Column(name = "keywords", length = 1000)
    private String keywords;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    @Builder.Default
    @Column(name = "last_checked_timestamp")
    private LocalDateTime lastCheckedTimestamp = LocalDateTime.now();

    public void updateSettings(String mmURL, String notificationENV, String keywords, Boolean isEnabled) {
        this.mmURL = mmURL;
        this.notificationENV = notificationENV;
        this.keywords = keywords;
        this.isEnabled = isEnabled;
    }

    public void updateLastCheckedTimestamp(LocalDateTime timestamp) { this.lastCheckedTimestamp = timestamp; }
}
