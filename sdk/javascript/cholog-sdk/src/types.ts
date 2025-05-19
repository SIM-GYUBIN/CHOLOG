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
  method: string;
  requestUri: string;
  status?: number;
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
