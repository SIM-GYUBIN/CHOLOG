// src/core/logger.ts
var Logger = class {
  static {
    this.appKey = "";
  }
  static {
    // private static apiEndpoint = "https://www.cholog-server.shop/log";
    this.apiEndpoint = "http://localhost:8080/logs";
  }
  static {
    this.originalConsole = null;
  }
  static {
    this.logQueue = [];
  }
  static {
    this.batchInterval = 1e3;
  }
  static {
    // 기본 1초
    this.batchTimeoutId = null;
  }
  static {
    this.maxQueueSize = 100 * 1024;
  }
  static {
    // 기본 100KB
    this.currentQueueSize = 0;
  }
  /**
   * SDK 초기화
   */
  static init(config) {
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
  static overrideConsoleMethods() {
    this.originalConsole = {
      log: console.log.bind(console),
      info: console.info.bind(console),
      warn: console.warn.bind(console),
      error: console.error.bind(console),
      debug: console.debug.bind(console),
      trace: console.trace.bind(console)
    };
    console.log = (...args) => this.queueAndPrint("info", args);
    console.info = (...args) => this.queueAndPrint("info", args);
    console.warn = (...args) => this.queueAndPrint("warn", args);
    console.error = (...args) => this.queueAndPrint("error", args);
    console.debug = (...args) => this.queueAndPrint("debug", args);
    console.trace = (...args) => this.queueAndPrint("trace", args);
  }
  /** 원본 콘솔 출력 + 큐잉 */
  static queueAndPrint(level, args) {
    if (this.originalConsole) {
      this.originalConsole[level](...args);
    }
    this.queueLog(level, args);
  }
  /** 로그를 큐에 쌓고, 배치 전송 스케줄링 */
  static queueLog(level, args) {
    const message = args.map(
      (arg) => typeof arg === "object" ? JSON.stringify(arg) : String(arg)
    ).join(" ");
    const entry = {
      level,
      message,
      timestamp: (/* @__PURE__ */ new Date()).toISOString()
    };
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
  static scheduleBatch() {
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
  static async sendBatch() {
    if (this.logQueue.length === 0) return;
    const batch = [...this.logQueue];
    this.logQueue = [];
    this.currentQueueSize = 0;
    try {
      const res = await fetch(this.apiEndpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "App-Key": this.appKey
        },
        body: JSON.stringify(batch)
      });
      if (!res.ok) {
        throw new Error(`Log send failed: ${res.status}`);
      }
    } catch (err) {
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
  /**
   * 자체 로거 메서드 (콘솔 출력 X)
   */
  /**
   * INFO 레벨 로그를 Cholog 서버로 전송합니다. (콘솔 출력 없음)
   * @param args 로그 데이터
   */
  static info(...args) {
    if (!this.appKey) {
      console.warn("Cholog SDK is not initialized. Call ChologSDK.init first.");
      return;
    }
    this.queueLog("info", args);
  }
  /**
   * WARN 레벨 로그를 Cholog 서버로 전송합니다. (콘솔 출력 없음)
   * @param args 로그 데이터
   */
  static warn(...args) {
    if (!this.appKey) {
      console.warn("Cholog SDK is not initialized. Call ChologSDK.init first.");
      return;
    }
    this.queueLog("warn", args);
  }
  /**
   * ERROR 레벨 로그를 Cholog 서버로 전송합니다. (콘솔 출력 없음)
   * @param args 로그 데이터
   */
  static error(...args) {
    if (!this.appKey) {
      console.warn("Cholog SDK is not initialized. Call ChologSDK.init first.");
      return;
    }
    this.queueLog("error", args);
  }
  /**
   * DEBUG 레벨 로그를 Cholog 서버로 전송합니다. (콘솔 출력 없음)
   * @param args 로그 데이터
   */
  static debug(...args) {
    if (!this.appKey) {
      console.warn("Cholog SDK is not initialized. Call ChologSDK.init first.");
      return;
    }
    this.queueLog("debug", args);
  }
  /**
   * TRACE 레벨 로그를 Cholog 서버로 전송합니다. (콘솔 출력 없음)
   * @param args 로그 데이터
   */
  static trace(...args) {
    if (!this.appKey) {
      console.warn("Cholog SDK is not initialized. Call ChologSDK.init first.");
      return;
    }
    this.queueLog("trace", args);
  }
};

// src/core/networkInterceptor.ts
var NetworkInterceptor = class {
  static init(config) {
    console.log("Network Interceptor initialized");
  }
};

// src/core/errorCatcher.ts
var ErrorCatcher = class {
  static init() {
    console.log("Error Catcher initialized");
  }
};

// src/core/eventTracker.ts
var EventTracker = class {
  static init() {
    console.log("Event Tracker initialized");
  }
};

// src/index.ts
var Cholog = {
  init: (config) => {
    Logger.init(config);
    NetworkInterceptor.init(config);
    ErrorCatcher.init();
    EventTracker.init();
  },
  log: Logger.info.bind(Logger),
  info: Logger.info.bind(Logger),
  warn: Logger.warn.bind(Logger),
  error: Logger.error.bind(Logger),
  debug: Logger.debug.bind(Logger),
  trace: Logger.trace.bind(Logger)
};
var index_default = Cholog;
export {
  Cholog,
  index_default as default
};
