interface LogEntry {
  level: string;
  message: string;
  timestamp: string;
}

type LogLevel = "log" | "info" | "warn" | "error" | "debug" | "trace";

export class Logger {
  private static appKey: string = "";
  private static apiEndpoint = "http://localhost:8080/logs";
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
    appKey: string;
    apiEndpoint?: string;
    batchInterval?: number;
    maxQueueSize?: number;
  }): void {
    // 중복 초기화 방지
    if (this.originalConsole !== null) {
      console.warn("Logger already initialized.");
      return;
    }

    this.appKey = config.appKey;
    if (config.apiEndpoint) this.apiEndpoint = config.apiEndpoint;
    if (config.batchInterval) this.batchInterval = config.batchInterval;
    if (config.maxQueueSize) this.maxQueueSize = config.maxQueueSize;

    this.overrideConsoleMethods();
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

    console.log = (...args) => this.queueAndPrint("info", args);
    console.info = (...args) => this.queueAndPrint("info", args);
    console.warn = (...args) => this.queueAndPrint("warn", args);
    console.error = (...args) => this.queueAndPrint("error", args);
    console.debug = (...args) => this.queueAndPrint("debug", args);
    console.trace = (...args) => this.queueAndPrint("trace", args);
  }

  /** 원본 콘솔 출력 + 큐잉 */
  private static queueAndPrint(level: LogLevel, args: any[]): void {
    if (this.originalConsole) {
      this.originalConsole[level](...args);
    }
    this.queueLog(level, args);
  }

  /** 로그를 큐에 쌓고, 배치 전송 스케줄링 */
  private static queueLog(level: LogLevel, args: any[]): void {
    const message = args
      .map((arg) =>
        typeof arg === "object" ? JSON.stringify(arg) : String(arg)
      )
      .join(" ");
    const entry: LogEntry = {
      level,
      message,
      timestamp: new Date().toISOString(),
    };

    // 대략적인 사이즈 계산
    const size = new Blob([JSON.stringify(entry)]).size;
    this.logQueue.push(entry);
    this.currentQueueSize += size;

    if (this.currentQueueSize > this.maxQueueSize) {
      // 큐가 너무 커지면 즉시 전송
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
          "App-Key": this.appKey,
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
        console.error(
          "Logger sendBatch error (original console unavailable):",
          err
        );
      }
    }
  }
}
