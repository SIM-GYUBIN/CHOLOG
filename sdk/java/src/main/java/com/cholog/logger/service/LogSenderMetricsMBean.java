package com.cholog.logger.service;

/**
 * LogSenderService의 JMX를 통한 모니터링을 위한 MBean 인터페이스입니다.
 * 로그 전송 서비스의 다양한 성능 지표와 상태 정보를 노출합니다.
 */
public interface LogSenderMetricsMBean {
    
    /**
     * 현재 메모리 큐에 대기 중인 로그 메시지 수를 반환합니다.
     * 
     * @return 큐에 대기 중인 로그 수
     */
    int getQueueSize();
    
    /**
     * 서비스 시작 이후 성공적으로 전송된 로그 메시지의 총 수를 반환합니다.
     * 
     * @return 성공적으로 전송된 총 로그 수
     */
    long getTotalProcessedLogs();
    
    /**
     * 서비스 시작 이후 전송에 실패하여 디스크에 저장된 로그 메시지의 총 수를 반환합니다.
     * 
     * @return 디스크에 저장된 총 로그 수
     */
    long getTotalFailedLogs();
    
    /**
     * 현재 로그 서버 연결 상태를 반환합니다.
     * 
     * @return 서버가 연결되어 있으면 true, 그렇지 않으면 false
     */
    boolean isServerConnected();
    
    /**
     * 서비스 시작 이후 경과한 시간(초)을 반환합니다.
     * 
     * @return 서비스 실행 시간(초)
     */
    long getUptimeSeconds();
    
    /**
     * 초당 평균 처리 로그 수를 반환합니다.
     * 
     * @return 초당 처리된 로그 메시지 수
     */
    double getLogsPerSecond();
    
    /**
     * 디스크 큐에 현재 저장된 파일 수를 반환합니다.
     * 
     * @return 디스크 큐의 파일 수
     */
    int getDiskQueueFileCount();
    
    /**
     * 디스크 큐의 현재 사용 크기(바이트)를 반환합니다.
     * 
     * @return 디스크 큐 크기(바이트)
     */
    long getDiskQueueSizeBytes();
} 