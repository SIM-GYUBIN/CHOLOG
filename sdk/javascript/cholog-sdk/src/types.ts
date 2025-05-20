// src/types.ts

export type LogLevelType = "INFO" | "WARN" | "ERROR" | "DEBUG" | "TRACE";
export type LogType = "general" | "error" | "network" | "event";

export interface LogPayload {
  [key: string]: any;
}

export interface LogError {
  type: string;
  message: string;
  stacktrace?: string;
}

export interface LogHttp {
  requestMethod: string;
  requestUri: string;
  httpStatus?: number;
  responseTime?: number;
}

export interface LogClient {
  url: string;
  userAgent: string;
  referrer?: string;
}

export interface LogEvent {
  type: string;
  targetSelector?: string;
  properties?: Record<string, any>;
}

export interface LogEntry {
  timestamp: string;
  sequence: number;
  level: string;
  message: string;
  source: "frontend" | "backend";
  projectKey: string;
  environment: string;
  requestId: string | null;

  logger: "console" | "cholog";
  logType: LogType;

  payload?: LogPayload;
  error?: LogError;
  http?: LogHttp;
  client?: LogClient;
  event?: LogEvent;
}

// Cholog SDK 초기화 설정을 위한 인터페이스
export interface ChologConfig {
  apiKey: string;
  environment?: string; // 필수 값으로 유지하거나, Logger처럼 내부 기본값 설정 가능
  enableEventTracker?: boolean; // 기본값 true
  enableErrorCatcher?: boolean; // 기본값 true
  enableNetworkInterceptor?: boolean; // 기본값 true
  // Logger 관련 상세 설정 (기존 Logger.init의 config와 유사)
  loggerOptions?: {
    batchInterval?: number;
    maxQueueSize?: number;
  };
}
