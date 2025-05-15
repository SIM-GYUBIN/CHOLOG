package com.ssafy.cholog.domain.report.dto.item;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimeSlotLogCount {
    private String timeSlot; // 시간대 (예: "2025-05-07T10:00:00Z", 또는 "10시-11시")
    private long count;      // 해당 시간대의 로그 발생 건수
}
