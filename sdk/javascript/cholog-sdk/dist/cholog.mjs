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
  static {
    this.isInitialized = false;
  }
  static {
    // 원래 함수들을 저장할 변수
    this.originalFetch = null;
  }
  static {
    this.originalXhrSend = null;
  }
  // 필요시 open도 저장: private static originalXhrOpen: typeof XMLHttpRequest.prototype.open | null = null;
  static generateRequestId() {
    if (crypto && crypto.randomUUID) {
      return crypto.randomUUID();
    } else {
      console.warn(
        "crypto.randomUUID is not available. Using basic fallback for Request ID."
      );
      return `fallback-${Date.now()}-${Math.random().toString(36).substring(2, 15)}`;
    }
  }
  /**
   * window.fetch를 패치하여 X-Request-ID 헤더를 추가
   */
  static patchFetch() {
    this.originalFetch = window.fetch;
    const self = this;
    window.fetch = async (input, init) => {
      const requestId = self.generateRequestId();
      const modifiedInit = { ...init || {} };
      let currentHeaders = modifiedInit.headers;
      let newHeaders;
      if (currentHeaders instanceof Headers) {
        newHeaders = new Headers(currentHeaders);
      } else if (Array.isArray(currentHeaders)) {
        newHeaders = new Headers(currentHeaders);
      } else if (typeof currentHeaders === "object" && currentHeaders !== null) {
        newHeaders = new Headers(currentHeaders);
      } else {
        newHeaders = new Headers();
      }
      newHeaders.set("X-Request-ID", requestId);
      modifiedInit.headers = newHeaders;
      if (!self.originalFetch) {
        console.error("Original fetch function not found!");
        return Promise.reject(new Error("Original fetch not available"));
      }
      return self.originalFetch.call(window, input, modifiedInit);
    };
  }
  /**
   * XMLHttpRequest.prototype.send를 패치하여 X-Request-ID 헤더를 추가
   */
  static patchXMLHttpRequest() {
    this.originalXhrSend = XMLHttpRequest.prototype.send;
    const self = this;
    XMLHttpRequest.prototype.send = function(body) {
      const requestId = self.generateRequestId();
      try {
        this.setRequestHeader("X-Request-ID", requestId);
      } catch (e) {
        console.error(
          "Cholog SDK: Failed to set X-Request-ID header. Was XHR opened first?",
          e
        );
      }
      if (!self.originalXhrSend) {
        console.error("Original XHR send function not found!");
        return;
      }
      return self.originalXhrSend.apply(this, arguments);
    };
  }
  /**
   * Network Interceptor를 초기화
   * fetch와 XMLHttpRequest에 대한 패치를 적용
   */
  static init() {
    if (this.isInitialized) {
      console.warn("NetworkInterceptor is already initialized.");
      return;
    }
    if (typeof window === "undefined" || typeof XMLHttpRequest === "undefined") {
      console.warn(
        "NetworkInterceptor: Not running in a browser environment? Skipping patch."
      );
      return;
    }
    try {
      this.patchFetch();
      this.patchXMLHttpRequest();
      this.isInitialized = true;
      console.log("Cholog NetworkInterceptor initialized successfully.");
    } catch (error) {
      console.error(
        "Cholog SDK: Failed to initialize NetworkInterceptor.",
        error
      );
    }
  }
  /**
   * 패치된 함수들을 원래대로 복원
   */
  // public static restore(): void {
  //   if (!this.isInitialized) return;
  //   if (this.originalFetch) {
  //     window.fetch = this.originalFetch;
  //   }
  //   if (this.originalXhrSend) {
  //     XMLHttpRequest.prototype.send = this.originalXhrSend;
  //   }
  //   // if (this.originalXhrOpen) { XMLHttpRequest.prototype.open = this.originalXhrOpen; } // open도 복원
  //   this.originalFetch = null;
  //   this.originalXhrSend = null;
  //   // this.originalXhrOpen = null;
  //   this.isInitialized = false;
  //   console.log("Cholog NetworkInterceptor restored original functions."); // 복원 로그 (선택 사항)
  // }
};

// src/core/errorCatcher.ts
var ErrorCatcher = class {
  static {
    this.isInitialized = false;
  }
  static {
    // window.onerror 또는 addEventListener('error') 핸들러
    // event: 오류 이벤트 객체 또는 메시지 문자열
    // source: 파일 URL
    // lineno: 줄 번호
    // colno: 컬럼 번호
    // error: 실제 Error 객체 (최신 브라우저에서 제공)
    this.handleGlobalError = (event, source, lineno, colno, error) => {
      let message;
      let filename = source;
      let line = lineno;
      let column = colno;
      let stack;
      let errorType;
      let errorObj = error;
      if (event instanceof ErrorEvent && event.error) {
        errorObj = event.error;
        message = event.message || errorObj?.message || "Error message not available";
        filename = event.filename;
        line = event.lineno;
        column = event.colno;
        errorType = errorObj?.name;
        stack = errorObj?.stack;
      } else if (typeof event === "string") {
        message = event;
        if (errorObj) {
          errorType = errorObj.name;
          stack = errorObj.stack;
        }
      } else if (errorObj) {
        message = errorObj.message;
        errorType = errorObj.name;
        stack = errorObj.stack;
      } else {
        message = "A non-error event was captured by the error handler.";
        errorType = "UnknownError";
      }
      if (stack?.includes("cholog") || message?.includes("Cholog SDK")) {
        console.warn(
          "Cholog SDK: Suppressed potential recursive error log.",
          message
        );
        return;
      }
      const details = {
        errorType: errorType || "Error",
        stack,
        sourceFile: filename,
        lineno: line,
        colno: column,
        userAgent: navigator.userAgent,
        pageUrl: window.location.href
      };
      Logger.error(message || "Uncaught JavaScript Error", details);
    };
  }
  static {
    // unhandledrejection 핸들러
    this.handleUnhandledRejection = (event) => {
      let reason = event.reason;
      let message;
      let stack;
      let errorType;
      if (reason instanceof Error) {
        message = reason.message;
        stack = reason.stack;
        errorType = reason.name;
      } else {
        try {
          message = `Unhandled promise rejection: ${JSON.stringify(reason)}`;
        } catch {
          message = `Unhandled promise rejection: [Non-serializable reason]`;
        }
        errorType = typeof reason;
      }
      if (stack?.includes("cholog") || message?.includes("Cholog SDK")) {
        console.warn(
          "Cholog SDK: Suppressed potential recursive error log.",
          message
        );
        return;
      }
      const details = {
        errorType: errorType || "UnhandledRejection",
        stack,
        // Promise 오류는 특정 파일/줄번호를 알기 어려울 수 있음
        sourceFile: stack ? void 0 : window.location.href,
        // 스택이 없으면 현재 URL
        userAgent: navigator.userAgent,
        pageUrl: window.location.href,
        reason: !(reason instanceof Error) ? String(reason) : void 0
        // Error 객체 아닌 경우만 reason 추가
      };
      Logger.error(message || "Unhandled Promise Rejection", details);
    };
  }
  static init() {
    if (this.isInitialized || typeof window === "undefined") {
      return;
    }
    try {
      window.onerror = this.handleGlobalError;
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
    NetworkInterceptor.init();
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
