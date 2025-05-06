// src/types.ts (신규 파일)
export interface LogPayload {
  [key: string]: any;
}

export interface LogError {
  type: string;
  message: string;
  stacktrace?: string;
}

export interface LogHttpRequest {
  method: string;
  url: string;
}

export interface LogHttpResponse {
  statusCode: number;
}

export interface LogHttp {
  request: LogHttpRequest;
  response?: LogHttpResponse; // 응답은 없을 수도 있음 (요청 실패 등)
  durationMs?: number;
}

export interface LogClient {
  url: string;
  userAgent: string;
  referrer?: string;
}

export interface LogEvent {
  type: string;
  targetSelector?: string;
}

export interface LogEntry {
  timestamp: string;
  level: string;
  message: string;
  source: "frontend" | "backend";
  projectKey: string;
  environment: string;
  traceId: string | null;

  payload?: LogPayload;
  error?: LogError;
  http?: LogHttp;
  client?: LogClient;
  event?: LogEvent;
  loggerName?: string;
}
