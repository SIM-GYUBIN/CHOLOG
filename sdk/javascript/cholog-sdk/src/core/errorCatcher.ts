// src/core/errorCatcher.ts
import { Logger } from "./logger";
import { LogError, LogClient } from "../types";
// import { TraceContext } from "./traceContext";

// 오류 로그를 위한 구체적인 데이터 구조 (선택 사항, Logger.error 인자로 바로 객체 전달도 가능)
interface ErrorLogDetails {
  errorType?: string; // 오류 타입 (e.g., 'TypeError')
  stack?: string; // 스택 트레이스
  sourceFile?: string; // 발생 파일
  lineno?: number; // 발생 줄
  colno?: number; // 발생 컬럼
  userAgent: string; // 브라우저 정보
  pageUrl: string; // 현재 페이지 URL
  reason?: string; // (UnhandledRejection 경우) 거부 이유
  traceId?: string | null;
}

export class ErrorCatcher {
  private static isInitialized = false;

  private static handleGlobalError = (
    eventOrMessage: ErrorEvent | Event | string,
    source?: string,
    lineno?: number,
    colno?: number,
    errorObj?: Error
  ): void => {
    let logMessage: string = "Unhandled global error";
    const errorDetails: LogError = { type: "UnknownError", message: "" };
    const clientDetails: Partial<LogClient> = {}; // Logger가 기본 client 정보를 채우므로 여기선 최소화

    let actualError: Error | undefined = errorObj;

    if (eventOrMessage instanceof ErrorEvent && eventOrMessage.error) {
      actualError = eventOrMessage.error;
      logMessage = eventOrMessage.message || actualError?.message || "Error message not available";
      errorDetails.type = actualError?.name || "ErrorEvent";
      errorDetails.message = actualError?.message || logMessage;
      if (actualError?.stack) errorDetails.stacktrace = actualError.stack;
      // filename, lineno, colno는 stacktrace에 보통 포함됨
    } else if (typeof eventOrMessage === "string") {
      logMessage = eventOrMessage;
      errorDetails.message = eventOrMessage;
      if (actualError) {
        errorDetails.type = actualError.name;
        errorDetails.message = actualError.message; // 중복될 수 있지만 명시
        if (actualError.stack) errorDetails.stacktrace = actualError.stack;
      }
    } else if (actualError) {
      // errorObj가 제공된 경우
      logMessage = actualError.message;
      errorDetails.type = actualError.name;
      errorDetails.message = actualError.message;
      if (actualError.stack) errorDetails.stacktrace = actualError.stack;
    }
    // 기타 경우는 기본값 사용

    // SDK 내부 오류 재귀 방지 (간단 체크)
    if (errorDetails.stacktrace?.includes("cholog") || logMessage?.includes("Cholog SDK")) {
      Logger["originalConsole"]?.warn?.("Cholog SDK: Suppressed potential recursive error log.", logMessage);
      return;
    }

    // Logger.logError는 Logger 내부에서 client 정보를 자동으로 채움
    Logger.logError(logMessage, errorDetails);
  };

  private static handleUnhandledRejection = (event: PromiseRejectionEvent): void => {
    let reason = event.reason;
    let logMessage: string = "Unhandled promise rejection";
    const errorDetails: LogError = { type: "UnhandledRejection", message: "" };

    if (reason instanceof Error) {
      logMessage = reason.message || "Promise rejected with an Error";
      errorDetails.type = reason.name || "UnhandledRejectionError";
      errorDetails.message = reason.message;
      if (reason.stack) errorDetails.stacktrace = reason.stack;
    } else {
      try {
        errorDetails.message = `Reason: ${JSON.stringify(reason)}`;
      } catch {
        errorDetails.message = `Reason: [Non-serializable]`;
      }
      logMessage = `Unhandled promise rejection: ${errorDetails.message}`; // 메시지 업데이트
    }

    if (errorDetails.stacktrace?.includes("cholog") || logMessage?.includes("Cholog SDK")) {
      Logger["originalConsole"]?.warn?.("Cholog SDK: Suppressed potential recursive error log.", logMessage);
      return;
    }

    Logger.logError(logMessage, errorDetails);
  };

  public static init(): void {
    if (this.isInitialized || typeof window === "undefined") {
      return;
    }
    try {
      window.onerror = this.handleGlobalError;
      window.addEventListener("unhandledrejection", this.handleUnhandledRejection);
      this.isInitialized = true;
      // console.log("Cholog ErrorCatcher initialized successfully."); // 초기화 로그는 index.ts에서
    } catch (error) {
      console.error("Cholog SDK: Failed to initialize ErrorCatcher.", error);
    }
  }
}
