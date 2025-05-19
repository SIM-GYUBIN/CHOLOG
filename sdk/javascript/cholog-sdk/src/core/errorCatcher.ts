// src/core/errorCatcher.ts
import { Logger } from "./logger";
import { LogError, LogClient } from "../types"; // LogClient 타입 추가

export class ErrorCatcher {
  private static isInitialized = false;

  // handleGlobalError 및 handleUnhandledRejection 메서드는 거의 동일하게 유지 가능
  // Logger.logError 호출 부분이 이미 새로운 시그니처와 잘 맞음
  private static handleGlobalError = (
    eventOrMessage: ErrorEvent | Event | string,
    source?: string, // 이 인자들은 이제 ErrorEvent 객체나 errorObj에서 추출
    lineno?: number,
    colno?: number,
    errorObj?: Error // window.onerror의 마지막 인자
  ): void => {
    let logMessage: string = "처리되지 않은 전역 오류";
    const errorDetails: LogError = { type: "UnknownError", message: "" };
    const clientDetails: Partial<LogClient> = {}; // Logger가 기본 수집

    let actualError: Error | undefined = errorObj;

    if (eventOrMessage instanceof ErrorEvent && eventOrMessage.error) {
      actualError = eventOrMessage.error;
      logMessage = eventOrMessage.message || actualError?.message || "오류 메시지를 사용할 수 없음";
      errorDetails.type = actualError?.name || "ErrorEvent";
      errorDetails.message = actualError?.message || logMessage;
      if (actualError?.stack) errorDetails.stacktrace = actualError.stack;
      // filename, lineno, colno는 ErrorEvent 객체에 직접 있음
      // errorDetails.sourceFunction = eventOrMessage.filename;
      // errorDetails.lineNumber = eventOrMessage.lineno;
      // errorDetails.columnNumber = eventOrMessage.colno;
    } else if (typeof eventOrMessage === "string") {
      logMessage = eventOrMessage;
      errorDetails.message = eventOrMessage;
      if (actualError) {
        // errorObj가 string 메시지와 함께 제공된 경우
        errorDetails.type = actualError.name;
        // errorDetails.message = actualError.message; // logMessage로 이미 설정됨
        if (actualError.stack) errorDetails.stacktrace = actualError.stack;
      } else {
        // 오류 객체 없이 메시지만 온 경우 (드묾)
        errorDetails.type = "GenericError";
        if (source) errorDetails.message += ` in ${source}`; // 추가 정보
      }
    } else if (actualError) {
      // errorObj만 제공된 경우 (ErrorEvent가 아닌 일반 Event와 Error 객체)
      logMessage = actualError.message;
      errorDetails.type = actualError.name;
      errorDetails.message = actualError.message;
      if (actualError.stack) errorDetails.stacktrace = actualError.stack;
    }

    // SDK 내부 오류 재귀 방지 (간단 체크)
    if (errorDetails.stacktrace?.includes("cholog") || logMessage?.includes("Cholog SDK")) {
      // Logger가 초기화되지 않았을 수 있으므로 console 직접 사용
      console.warn("[CHO:LOG] SDK 내부에서 발생한 잠재적 재귀 오류를 차단했습니다.", logMessage);
      return;
    }

    // Logger.logError 호출 시 clientDetails는 선택사항 (Logger가 자동 수집)
    // 필요시 여기서 특정 client 정보를 채워 넘길 수 있음
    if (typeof window !== "undefined") {
      clientDetails.url = window.location.href;
      // userAgent는 Logger가 채우므로 중복 불필요
    }
    Logger.logError(logMessage, errorDetails, clientDetails as LogClient);
  };

  private static handleUnhandledRejection = (event: PromiseRejectionEvent): void => {
    let reason = event.reason;
    let logMessage: string = "처리되지 않은 프로미스 거부";
    const errorDetails: LogError = { type: "UnhandledRejection", message: "" };
    const clientDetails: Partial<LogClient> = {};

    if (reason instanceof Error) {
      logMessage = reason.message || "프로미스가 오류와 함께 거부됨";
      errorDetails.type = reason.name || "UnhandledRejectionError";
      errorDetails.message = reason.message;
      if (reason.stack) errorDetails.stacktrace = reason.stack;
    } else {
      try {
        errorDetails.message = `원인: ${JSON.stringify(reason)}`;
      } catch {
        errorDetails.message = `원인: [직렬화 불가능]`;
      }
      logMessage = `Unhandled promise rejection: ${errorDetails.message}`;
    }

    if (errorDetails.stacktrace?.includes("cholog") || logMessage?.includes("Cholog SDK")) {
      console.warn("[CHO:LOG] SDK 내부에서 발생한 잠재적 재귀 오류를 차단했습니다.", logMessage);
      return;
    }

    if (typeof window !== "undefined") {
      clientDetails.url = window.location.href;
    }
    Logger.logError(logMessage, errorDetails, clientDetails as LogClient);
  };

  public static init(): void {
    if (this.isInitialized || typeof window === "undefined") {
      return;
    }
    try {
      window.onerror = this.handleGlobalError;
      window.addEventListener("unhandledrejection", this.handleUnhandledRejection);
      this.isInitialized = true;
    } catch (error) {
      console.error("[CHO:LOG] SDK: ErrorCatcher 초기화에 실패했습니다.", error);
    }
  }
}
