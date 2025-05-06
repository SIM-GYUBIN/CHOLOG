// src/core/errorCatcher.ts
import { Logger } from "./logger";

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
}

export class ErrorCatcher {
  private static isInitialized = false;

  // window.onerror 또는 addEventListener('error') 핸들러
  // event: 오류 이벤트 객체 또는 메시지 문자열
  // source: 파일 URL
  // lineno: 줄 번호
  // colno: 컬럼 번호
  // error: 실제 Error 객체 (최신 브라우저에서 제공)
  private static handleGlobalError = (
    event: ErrorEvent | Event | string,
    source?: string,
    lineno?: number,
    colno?: number,
    error?: Error
  ): void => {
    // console.log("handleGlobalError triggered:", event); // 디버깅용

    let message: string;
    let filename = source;
    let line = lineno;
    let column = colno;
    let stack: string | undefined;
    let errorType: string | undefined;
    let errorObj: Error | undefined = error; // 실제 Error 객체

    // ErrorEvent 인스턴스인지 확인 (addEventListener('error',...) 사용 시)
    if (event instanceof ErrorEvent && event.error) {
      errorObj = event.error;
      message =
        event.message || errorObj?.message || "Error message not available";
      filename = event.filename;
      line = event.lineno;
      column = event.colno;
      errorType = errorObj?.name;
      stack = errorObj?.stack;
    } else if (typeof event === "string") {
      // window.onerror 사용 시 (메시지만 문자열로 올 경우)
      message = event;
      // error 객체가 5번째 인자로 넘어올 수 있음
      if (errorObj) {
        errorType = errorObj.name;
        stack = errorObj.stack;
      }
    } else if (errorObj) {
      // window.onerror에서 error 객체가 제공된 경우
      message = errorObj.message;
      errorType = errorObj.name;
      stack = errorObj.stack;
    } else {
      // 리소스 로딩 오류 등 Error 객체가 없는 경우 (img, script 로드 실패 등)
      // 필요하다면 event.target 등을 분석하여 정보를 기록할 수 있으나,
      // 보통 JS 오류와는 성격이 다르므로 여기서는 상세 로깅 생략하거나 별도 처리.
      // console.warn("Cholog SDK: Non-JavaScript error event detected, skipping detailed logging.", event);
      // return; // 필요 없으면 여기서 처리 중단
      message = "A non-error event was captured by the error handler."; // 임시 메시지
      errorType = "UnknownError";
    }

    // SDK 내부 오류로 인한 무한 루프 방지 (간단 체크)
    if (stack?.includes("cholog") || message?.includes("Cholog SDK")) {
      console.warn(
        "Cholog SDK: Suppressed potential recursive error log.",
        message
      );
      return;
    }

    // 서버로 보낼 데이터 구성
    const details: ErrorLogDetails = {
      errorType: errorType || "Error",
      stack: stack,
      sourceFile: filename,
      lineno: line,
      colno: column,
      userAgent: navigator.userAgent,
      pageUrl: window.location.href,
    };

    // Logger 모듈을 사용하여 오류 전송 (error 레벨 사용)
    // Logger.error는 내부적으로 queueLog('error', ...) 호출
    Logger.error(message || "Uncaught JavaScript Error", details);

    // 기본 브라우저 콘솔에 오류가 표시되지 않도록 하려면 true 반환 (window.onerror 한정)
    // return true;
  };

  // unhandledrejection 핸들러
  private static handleUnhandledRejection = (
    event: PromiseRejectionEvent
  ): void => {
    // console.log("handleUnhandledRejection triggered:", event); // 디버깅용

    let reason = event.reason;
    let message: string;
    let stack: string | undefined;
    let errorType: string | undefined;

    if (reason instanceof Error) {
      // 이유가 Error 객체인 경우
      message = reason.message;
      stack = reason.stack;
      errorType = reason.name;
    } else {
      // 이유가 Error 객체가 아닌 경우 (문자열, 숫자 등)
      try {
        message = `Unhandled promise rejection: ${JSON.stringify(reason)}`;
      } catch {
        message = `Unhandled promise rejection: [Non-serializable reason]`;
      }
      errorType = typeof reason;
    }

    // SDK 내부 오류 무한 루프 방지
    if (stack?.includes("cholog") || message?.includes("Cholog SDK")) {
      console.warn(
        "Cholog SDK: Suppressed potential recursive error log.",
        message
      );
      return;
    }

    const details: ErrorLogDetails = {
      errorType: errorType || "UnhandledRejection",
      stack: stack,
      // Promise 오류는 특정 파일/줄번호를 알기 어려울 수 있음
      sourceFile: stack ? undefined : window.location.href, // 스택이 없으면 현재 URL
      userAgent: navigator.userAgent,
      pageUrl: window.location.href,
      reason: !(reason instanceof Error) ? String(reason) : undefined, // Error 객체 아닌 경우만 reason 추가
    };

    // Logger 모듈을 사용하여 오류 전송
    Logger.error(message || "Unhandled Promise Rejection", details);
  };

  public static init(): void {
    if (this.isInitialized || typeof window === "undefined") {
      return;
    }

    try {
      // 1. Uncaught JavaScript Errors 처리
      // window.onerror가 addEventListener보다 먼저 실행될 수 있고,
      // error 객체를 5번째 인자로 받을 수 있는 장점이 있어 우선 사용.
      window.onerror = this.handleGlobalError;

      // 필요하다면 addEventListener('error')도 사용할 수 있으나, 중복 호출될 수 있음.
      // 리소스 로딩 오류(<img>, <script> 실패 등)를 잡으려면 addEventListener 필요.
      // window.addEventListener('error', this.handleGlobalError, true); // 캡처 단계에서 실행

      // 2. Unhandled Promise Rejections 처리
      window.addEventListener(
        "unhandledrejection",
        this.handleUnhandledRejection
      );

      this.isInitialized = true;
      console.log("Cholog ErrorCatcher initialized successfully.");
    } catch (error) {
      console.error("Cholog SDK: Failed to initialize ErrorCatcher.", error);
    }
  }

  // (선택 사항) 원래 핸들러로 복원하는 함수
  // public static restore(): void {
  //   if (!this.isInitialized || typeof window === "undefined") return;
  //   window.onerror = null;
  //   window.removeEventListener(
  //     "unhandledrejection",
  //     this.handleUnhandledRejection
  //   );
  //   // window.removeEventListener('error', this.handleGlobalError, true);
  //   this.isInitialized = false;
  //   console.log("Cholog ErrorCatcher restored original handlers.");
  // }
}
