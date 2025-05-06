// src/core/logger.ts
import { TraceContext } from "./traceContext";
import { LogEntry, LogPayload, LogError, LogHttp, LogClient, LogEvent } from "../types"; // 타입 임포트

type LogLevel = "info" | "warn" | "error" | "debug" | "trace"; // "log"는 info로 매핑됨

export class Logger {
  private static projectKey: string = "";
  private static environment: string = ""; // environment 필드 추가
  private static apiEndpoint = "http://localhost:8080/logs"; // 이전과 동일
  private static originalConsole: {
    log: Console["log"];
    info: Console["info"];
    warn: Console["warn"];
    error: Console["error"];
    debug: Console["debug"];
    trace: Console["trace"];
  } | null = null;
  private static logQueue: LogEntry[] = [];
  private static batchInterval = 1000; // 기본 1초
  private static batchTimeoutId: number | null = null;
  private static maxQueueSize = 100 * 1024; // 기본 100KB
  private static currentQueueSize = 0;

  /**
   * SDK 초기화
   */
  public static init(config: {
    projectKey: string;
    environment: string;
    batchInterval?: number;
    maxQueueSize?: number;
  }): void {
    // 중복 초기화 방지
    if (this.originalConsole !== null) {
      console.warn("Cholog: Logger already initialized.");
      return;
    }

    this.projectKey = config.projectKey;
    this.environment = config.environment;
    if (config.batchInterval) this.batchInterval = config.batchInterval;
    if (config.maxQueueSize) this.maxQueueSize = config.maxQueueSize;

    this.overrideConsoleMethods();
  }

  public static getApiEndpoint(): string {
    return this.apiEndpoint;
  }

  /** console 메서드 오버라이드 */
  private static overrideConsoleMethods(): void {
    this.originalConsole = {
      log: console.log.bind(console),
      info: console.info.bind(console),
      warn: console.warn.bind(console),
      error: console.error.bind(console),
      debug: console.debug.bind(console),
      trace: console.trace.bind(console),
    };

    console.log = (...args) => this.queueAndPrint("info", "console", ...args);
    console.info = (...args) => this.queueAndPrint("info", "console", ...args);
    console.warn = (...args) => this.queueAndPrint("warn", "console", ...args);
    console.error = (...args) => this.queueAndPrint("error", "console", ...args);
    console.debug = (...args) => this.queueAndPrint("debug", "console", ...args);
    console.trace = (...args) => this.queueAndPrint("trace", "console", ...args);
  }

  /** 원본 콘솔 출력 + 큐잉 */
  private static queueAndPrint(
    level: LogLevel, // 이제 "log"는 포함하지 않음
    loggerName: string,
    ...args: any[]
  ): void {
    if (this.originalConsole) {
      const originalMethod = this.originalConsole[level];
      if (originalMethod) {
        originalMethod(...args);
      } else {
        this.originalConsole.log(...args);
      }
    }
    this.prepareAndQueueLog(level, loggerName, args);
  }

  // 로그를 최종 구조로 만들고 큐에 넣는 핵심 메서드
  private static prepareAndQueueLog(
    level: LogLevel,
    loggerName: string,
    args: any[],
    // 다음 인자들은 특정 모듈에서 직접 구조화해서 넘겨줄 때 사용
    directError?: LogError,
    directHttp?: LogHttp,
    directClient?: LogClient,
    directEvent?: LogEvent
  ): void {
    if (!this.projectKey || !this.environment) {
      // 초기화되지 않았으면 원본 콘솔로 경고만 하고 로그 전송은 하지 않음 (무한 루프 방지)
      if (this.originalConsole) {
        this.originalConsole.warn("Cholog: SDK not initialized. Log not sent.", ...args);
      } else {
        console.warn("Cholog: SDK not initialized. Log not sent.", ...args);
      }
      return;
    }

    let message = "";
    let payload: LogPayload = {};
    const otherFields: Partial<Pick<LogEntry, "error" | "http" | "client" | "event">> = {};

    if (args.length > 0) {
      if (typeof args[0] === "string") {
        message = args[0];
        if (args.length > 1 && typeof args[1] === "object" && args[1] !== null) {
          // 첫번째 인자가 문자열, 두번째 인자가 객체면 payload로 간주
          // 단, ErrorCatcher 등에서 넘겨주는 구조화된 객체와 충돌하지 않도록 주의
          // 여기서는 console.log("message", {detail: "data"}) 같은 경우를 위함
          // ErrorCatcher 등에서는 args를 error 메시지 하나만 보내고, 나머지는 directXXX로 받음
          if (!directError && !directHttp && !directEvent) {
            payload = { ...args[1] }; // payload에 할당
          }
        } else if (args.length > 1) {
          // 첫번째 인자 외 추가 문자열들은 메시지에 포함
          message +=
            " " +
            args
              .slice(1)
              .map((arg) => (typeof arg === "object" ? JSON.stringify(arg) : String(arg)))
              .join(" ");
        }
      } else {
        // 첫번째 인자가 문자열이 아니면, 모든 인자를 문자열화하여 메시지로
        message = args.map((arg) => (typeof arg === "object" ? JSON.stringify(arg) : String(arg))).join(" ");
      }
    }

    // 직접 전달된 구조화된 데이터 할당
    if (directError) otherFields.error = directError;
    if (directHttp) otherFields.http = directHttp;
    if (directClient) otherFields.client = directClient; // 여러곳에서 client 정보를 넘길 수 있으므로 병합 필요할 수 있음
    if (directEvent) otherFields.event = directEvent;

    const entry: LogEntry = {
      timestamp: new Date().toISOString(),
      level: level.toUpperCase(),
      message,
      source: "frontend",
      projectKey: this.projectKey,
      environment: this.environment,
      traceId: TraceContext.getCurrentTraceId(),
      loggerName,
      ...otherFields, // error, http, client, event 객체 포함
    };

    // payload가 비어있지 않다면 추가
    if (Object.keys(payload).length > 0) {
      entry.payload = payload;
    }

    // client 정보는 항상 포함 (브라우저 환경에서만)
    if (typeof window !== "undefined" && typeof navigator !== "undefined" && typeof location !== "undefined") {
      if (!entry.client) entry.client = {} as LogClient; // 초기화
      entry.client.url = window.location.href;
      entry.client.userAgent = navigator.userAgent;
      if (document.referrer) {
        entry.client.referrer = document.referrer;
      }
    }

    const size = new Blob([JSON.stringify(entry)]).size;
    this.logQueue.push(entry);
    this.currentQueueSize += size;

    if (this.currentQueueSize > this.maxQueueSize) {
      this.sendBatch();
    } else {
      this.scheduleBatch();
    }
  }

  /** 일정 시간 후 배치 전송 예약 */
  private static scheduleBatch(): void {
    if (this.batchTimeoutId === null) {
      this.batchTimeoutId = window.setTimeout(async () => {
        await this.sendBatch();
        this.batchTimeoutId = null;
        if (this.logQueue.length > 0) {
          this.scheduleBatch();
        }
      }, this.batchInterval);
    }
  }

  /** 큐에 쌓인 로그를 서버로 전송 */
  private static async sendBatch(): Promise<void> {
    if (this.logQueue.length === 0) return;

    const batch = [...this.logQueue];
    this.logQueue = [];
    this.currentQueueSize = 0;

    try {
      const res = await fetch(this.apiEndpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "App-Key": this.projectKey,
        },
        body: JSON.stringify(batch),
      });
      if (!res.ok) {
        throw new Error(`Log send failed: ${res.status}`);
      }
    } catch (err) {
      // 전송 오류는 원본 콘솔로 출력
      if (this.originalConsole) {
        this.originalConsole.error("Logger sendBatch error:", err);
      } else {
        console.error("Logger sendBatch error (original console unavailable):", err);
      }
    }
  }

  // --- Cholog 자체 로거 메서드들 ---
  // Cholog.info("메시지", {부가정보객체}, {error객체}, {http객체} ...) 식으로 사용하지 않고,
  // 각 모듈(ErrorCatcher, NetworkInterceptor)에서 특화된 정보를 포함하여 로깅하도록 유도
  // 일반적인 사용: Cholog.info("단순 메시지") 또는 Cholog.info("메시지", {customPayload: "값"})

  public static log(message: string, customPayload?: LogPayload): void {
    this.prepareAndQueueLog("info", "cholog", [message, customPayload || {}]);
  }
  public static info(message: string, customPayload?: LogPayload): void {
    this.prepareAndQueueLog("info", "cholog", [message, customPayload || {}]);
  }
  public static warn(message: string, customPayload?: LogPayload): void {
    this.prepareAndQueueLog("warn", "cholog", [message, customPayload || {}]);
  }
  public static error(message: string, customPayload?: LogPayload): void {
    this.prepareAndQueueLog("error", "cholog", [message, customPayload || {}]);
  }
  public static debug(message: string, customPayload?: LogPayload): void {
    this.prepareAndQueueLog("debug", "cholog", [message, customPayload || {}]);
  }
  public static trace(message: string, customPayload?: LogPayload): void {
    this.prepareAndQueueLog("trace", "cholog", [message, customPayload || {}]);
  }

  // 에러 로깅 (ErrorCatcher에서 주로 사용)
  public static logError(errorMessage: string, errorDetails?: LogError, clientDetails?: LogClient): void {
    this.prepareAndQueueLog("error", "cholog-error", [errorMessage], errorDetails, undefined, clientDetails);
  }

  // 네트워크 로깅 (NetworkInterceptor에서 주로 사용)
  public static logHttp(
    message: string,
    httpDetails: LogHttp,
    clientDetails?: LogClient,
    errorDetails?: LogError
  ): void {
    const level = errorDetails || (httpDetails.response && httpDetails.response.statusCode >= 400) ? "error" : "info";
    this.prepareAndQueueLog(level, "cholog-network", [message], errorDetails, httpDetails, clientDetails);
  }

  // 이벤트 로깅 (EventTracker에서 주로 사용)
  public static logEvent(message: string, eventDetails: LogEvent, clientDetails?: LogClient): void {
    this.prepareAndQueueLog("info", "cholog-event", [message], undefined, undefined, clientDetails, eventDetails);
  }
}
