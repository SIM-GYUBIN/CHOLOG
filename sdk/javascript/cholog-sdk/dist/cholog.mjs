// src/core/requestContext.ts
var RequestContext = class {
  static {
    this.currentRequestId = null;
  }
  // private static currentSpanId: string | null = null; // 스팬 개념 도입 시
  static startNewRequest() {
    this.currentRequestId = this.generateId();
    return this.currentRequestId;
  }
  // 필요시 Span ID도 유사하게 관리
  // public static startNewSpan(parentId?: string): string {
  //     this.currentSpanId = this.generateId('span');
  //     // parentId를 사용하여 부모-자식 관계 설정 가능
  //     return this.currentSpanId;
  // }
  static getCurrentRequestId() {
    return this.currentRequestId;
  }
  static setCurrentRequestId(requestId) {
    this.currentRequestId = requestId;
  }
  static generateId() {
    if (typeof crypto !== "undefined" && crypto.randomUUID) {
      return crypto.randomUUID();
    }
    return `request-${Date.now()}-${Math.random().toString(36).substring(2, 11)}`;
  }
};

// src/core/logger.ts
var Logger = class {
  static {
    this.apiKey = "";
  }
  static {
    this.environment = "default";
  }
  static {
    // environment 필드 추가
    this.apiEndpoint = "https://cholog-server.shop/api/logs/js";
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
  static {
    this.logSequenceCounter = 0;
  }
  /**
   * SDK 초기화
   */
  static init(config) {
    if (this.originalConsole !== null) {
      console.warn("[CHO:LOG] \uC774\uBBF8 \uCD08\uAE30\uD654\uB41C \uC0C1\uD0DC\uC785\uB2C8\uB2E4..");
      return;
    }
    this.apiKey = config.apiKey;
    if (config.environment) this.environment = config.environment;
    if (config.batchInterval) this.batchInterval = config.batchInterval;
    if (config.maxQueueSize) this.maxQueueSize = config.maxQueueSize;
    this.overrideConsoleMethods();
  }
  static getApiEndpoint() {
    return this.apiEndpoint;
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
    console.log = (...args) => this.queueAndPrint("info", "console", ...args);
    console.info = (...args) => this.queueAndPrint("info", "console", ...args);
    console.warn = (...args) => this.queueAndPrint("warn", "console", ...args);
    console.error = (...args) => this.queueAndPrint("error", "console", ...args);
    console.debug = (...args) => this.queueAndPrint("debug", "console", ...args);
    console.trace = (...args) => this.queueAndPrint("trace", "console", ...args);
  }
  /** 원본 콘솔 출력 + 큐잉 */
  static queueAndPrint(level, invokedBy, ...args) {
    if (this.originalConsole) {
      const originalMethod = this.originalConsole[level];
      if (originalMethod) {
        originalMethod(...args);
      } else {
        this.originalConsole.log(...args);
      }
    }
    this.prepareAndQueueLog(level, invokedBy, args);
  }
  // 로그를 최종 구조로 만들고 큐에 넣는 핵심 메서드
  static prepareAndQueueLog(level, invokedBy, args, directError, directHttp, directClient, directEvent) {
    if (!this.apiKey || !this.environment) {
      if (this.originalConsole) {
        this.originalConsole.warn("[CHO:LOG] SDK\uAC00 \uCD08\uAE30\uD654\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4. \uB85C\uADF8\uAC00 \uC804\uC1A1\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.", ...args);
      } else {
        console.warn("[CHO:LOG] SDK\uAC00 \uCD08\uAE30\uD654\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4. \uB85C\uADF8\uAC00 \uC804\uC1A1\uB418\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.", ...args);
      }
      return;
    }
    let message = "";
    let payload = {};
    const otherFields = {};
    if (args.length > 0) {
      if (typeof args[0] === "string") {
        message = args[0];
        if (args.length > 1 && typeof args[1] === "object" && args[1] !== null) {
          if (!directError && !directHttp && !directEvent) {
            payload = { ...args[1] };
          }
        } else if (args.length > 1) {
          message += " " + args.slice(1).map((arg) => typeof arg === "object" ? JSON.stringify(arg) : String(arg)).join(" ");
        }
      } else {
        message = args.map((arg) => typeof arg === "object" ? JSON.stringify(arg) : String(arg)).join(" ");
      }
    }
    let determinedLogType;
    if (directError) {
      determinedLogType = "error";
    } else if (directHttp) {
      determinedLogType = "network";
    } else if (directEvent) {
      determinedLogType = "event";
    } else {
      determinedLogType = "general";
    }
    if (directError) otherFields.error = directError;
    if (directHttp) otherFields.http = directHttp;
    if (directClient) otherFields.client = directClient;
    if (directEvent) otherFields.event = directEvent;
    const currentSequence = this.logSequenceCounter++;
    const entry = {
      timestamp: (/* @__PURE__ */ new Date()).toISOString(),
      sequence: currentSequence,
      level: level.toUpperCase(),
      // LogLevelType으로 캐스팅
      message,
      source: "frontend",
      projectKey: this.apiKey,
      environment: this.environment,
      requestId: RequestContext.getCurrentRequestId(),
      logger: invokedBy,
      logType: determinedLogType,
      ...otherFields
    };
    if (Object.keys(payload).length > 0) {
      entry.payload = payload;
    }
    if (typeof window !== "undefined" && typeof navigator !== "undefined" && typeof location !== "undefined") {
      if (!entry.client) entry.client = {};
      entry.client.url = entry.client.url || window.location.href;
      entry.client.userAgent = entry.client.userAgent || navigator.userAgent;
      if (document.referrer && !entry.client.referrer) {
        entry.client.referrer = document.referrer;
      }
    }
    try {
      const size = new Blob([JSON.stringify(entry)]).size;
      this.logQueue.push(entry);
      this.currentQueueSize += size;
      if (this.currentQueueSize >= this.maxQueueSize) {
        this.sendBatch();
      } else {
        this.scheduleBatch();
      }
    } catch (e) {
      this.originalConsole?.error?.("[CHO:LOG] \uB85C\uADF8 \uD06C\uAE30 \uACC4\uC0B0 \uC911 \uC624\uB958 \uBC1C\uC0DD, \uAC1C\uC218 \uAE30\uBC18 \uB300\uAE30\uC5F4\uB85C \uC804\uD658\uD569\uB2C8\uB2E4.", e);
      this.logQueue.push(entry);
      if (this.logQueue.length > 20) {
        this.sendBatch();
      } else {
        this.scheduleBatch();
      }
    }
  }
  /** 일정 시간 후 배치 전송 예약 */
  static scheduleBatch() {
    if (this.batchTimeoutId === null && this.logQueue.length > 0) {
      this.batchTimeoutId = window.setTimeout(async () => {
        this.batchTimeoutId = null;
        await this.sendBatch();
        if (this.logQueue.length > 0) {
          this.scheduleBatch();
        }
      }, this.batchInterval);
    }
  }
  /** 큐에 쌓인 로그를 서버로 전송 */
  static async sendBatch() {
    if (this.logQueue.length === 0) return;
    if (this.batchTimeoutId !== null) {
      clearTimeout(this.batchTimeoutId);
      this.batchTimeoutId = null;
    }
    const batch = [...this.logQueue];
    this.logQueue = [];
    this.currentQueueSize = 0;
    try {
      const res = await fetch(this.apiEndpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "App-Key": this.apiKey
          // 필요시 서버와 협의된 인증 헤더 사용
        },
        body: JSON.stringify(batch)
      });
      if (!res.ok) {
        const errorText = await res.text();
        throw new Error(`Log send failed: ${res.status} - ${errorText}`);
      }
    } catch (err) {
      if (this.originalConsole) {
        this.originalConsole.error("[CHO:LOG] \uB85C\uAC70 \uBC30\uCE58 \uC804\uC1A1 \uC624\uB958:", err);
      } else {
        console.error("[CHO:LOG] \uB85C\uAC70 \uBC30\uCE58 \uC804\uC1A1 \uC624\uB958 (\uC6D0\uBCF8 \uCF58\uC194 \uC0AC\uC6A9 \uBD88\uAC00):", err);
      }
    }
  }
  // --- Cholog 자체 로거 메서드들 ---
  // Cholog.info("메시지", {부가정보객체}, {error객체}, {http객체} ...) 식으로 사용하지 않고,
  // 각 모듈(ErrorCatcher, NetworkInterceptor)에서 특화된 정보를 포함하여 로깅하도록 유도
  // 일반적인 사용: Cholog.info("단순 메시지") 또는 Cholog.info("메시지", {customPayload: "값"})
  static log(message, customPayload) {
    this.prepareAndQueueLog("info", "cholog", [message, customPayload || {}]);
  }
  static info(message, customPayload) {
    this.prepareAndQueueLog("info", "cholog", [message, customPayload || {}]);
  }
  static warn(message, customPayload) {
    this.prepareAndQueueLog("warn", "cholog", [message, customPayload || {}]);
  }
  static error(message, customPayload) {
    this.prepareAndQueueLog("error", "cholog", [message, customPayload || {}]);
  }
  static debug(message, customPayload) {
    this.prepareAndQueueLog("debug", "cholog", [message, customPayload || {}]);
  }
  static trace(message, customPayload) {
    this.prepareAndQueueLog("trace", "cholog", [message, customPayload || {}]);
  }
  // 에러 로깅 (ErrorCatcher에서 사용, logType: "error")
  static logError(errorMessage, errorDetails, clientDetails) {
    this.prepareAndQueueLog("error", "cholog", [errorMessage], errorDetails, void 0, clientDetails);
  }
  // 네트워크 로깅 (NetworkInterceptor에서 사용, logType: "network")
  static logHttp(message, httpDetails, clientDetails, errorDetails) {
    const level = errorDetails || httpDetails.httpStatus && httpDetails.httpStatus >= 400 ? "error" : "info";
    this.prepareAndQueueLog(level, "cholog", [message], errorDetails, httpDetails, clientDetails);
  }
  // 이벤트 로깅 (EventTracker에서 사용, logType: "event")
  static logEvent(message, eventDetails, clientDetails) {
    this.prepareAndQueueLog("info", "cholog", [message], void 0, void 0, clientDetails, eventDetails);
  }
};

// src/core/networkInterceptor.ts
var NetworkInterceptor = class _NetworkInterceptor {
  static {
    this.isInitialized = false;
  }
  static {
    this.originalFetch = null;
  }
  static {
    // XMLHttpRequest 관련 타입은 any로 처리하거나, 더 상세한 타입 정의 필요 시 추가
    this.originalXhrOpen = null;
  }
  static {
    this.originalXhrSend = null;
  }
  static patchFetch() {
    if (typeof window === "undefined" || !window.fetch) return;
    this.originalFetch = window.fetch;
    window.fetch = async (input, init) => {
      const requestUrl = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;
      if (requestUrl.startsWith(Logger.getApiEndpoint())) {
        return _NetworkInterceptor.originalFetch.call(window, input, init);
      }
      const requestId = RequestContext.getCurrentRequestId() || RequestContext.startNewRequest();
      const modifiedInit = { ...init || {} };
      modifiedInit.headers = new Headers(modifiedInit.headers);
      if (!modifiedInit.headers.has("X-Request-ID")) {
        modifiedInit.headers.set("X-Request-ID", requestId);
      }
      const startTime = Date.now();
      const requestDetails = {
        requestMethod: (modifiedInit.method || (typeof input !== "string" && !(input instanceof URL) ? input.method : "GET") || "GET").toUpperCase(),
        requestUri: requestUrl
      };
      try {
        const response = await _NetworkInterceptor.originalFetch.call(window, input, modifiedInit);
        const responseTime = Date.now() - startTime;
        Logger.logHttp(
          `Fetch \uC694\uCCAD => ${requestDetails.requestMethod} ${requestDetails.requestUri} - \uC0C1\uD0DC => ${response.status}`,
          {
            ...requestDetails,
            httpStatus: response.status,
            responseTime
          },
          void 0
          // clientDetails
        );
        return response;
      } catch (error) {
        const responseTime = Date.now() - startTime;
        const errorDetails = {
          type: error?.name || "FetchError",
          message: error?.message || "Network request failed",
          stacktrace: error?.stack
        };
        Logger.logHttp(
          `Fetch \uC624\uB958 => ${requestDetails.requestMethod} ${requestDetails.requestUri}`,
          {
            ...requestDetails,
            responseTime
          },
          void 0,
          // clientDetails
          errorDetails
        );
        throw error;
      }
    };
  }
  static patchXMLHttpRequest() {
    if (typeof window === "undefined" || !window.XMLHttpRequest) return;
    this.originalXhrOpen = XMLHttpRequest.prototype.open;
    this.originalXhrSend = XMLHttpRequest.prototype.send;
    const self = this;
    XMLHttpRequest.prototype.open = function(method, url) {
      this._chologMethod = method;
      this._chologUrl = typeof url === "string" ? url : url.toString();
      if (this._chologUrl.startsWith(Logger.getApiEndpoint())) {
        this._chologSkip = true;
      }
      self.originalXhrOpen.apply(this, arguments);
    };
    XMLHttpRequest.prototype.send = function(body) {
      if (this._chologSkip) {
        return self.originalXhrSend.apply(this, arguments);
      }
      const xhr = this;
      xhr._chologStartTime = Date.now();
      const requestId = RequestContext.getCurrentRequestId() || RequestContext.startNewRequest();
      this.setRequestHeader("X-Request-ID", requestId);
      const requestDetails = {
        requestMethod: (xhr._chologMethod || "UnknownMethod").toUpperCase(),
        requestUri: xhr._chologUrl || "UnknownURL"
      };
      const onLoadEnd = () => {
        if (xhr._chologLogged) return;
        xhr._chologLogged = true;
        const responseTime = xhr._chologStartTime ? Date.now() - xhr._chologStartTime : void 0;
        let errorDetails = void 0;
        if (xhr.status === 0 || xhr.status >= 400) {
          errorDetails = {
            type: xhr.statusText || "XHRError",
            message: `XHR \uC624\uB958 => ${requestDetails.requestUri} - \uC0C1\uD0DC => ${xhr.status || "N/A"}. ReadyState: ${xhr.readyState}`
          };
        }
        Logger.logHttp(
          `XHR \uC694\uCCAD => ${requestDetails.requestMethod} ${requestDetails.requestUri} - \uC0C1\uD0DC => ${xhr.status}`,
          {
            ...requestDetails,
            httpStatus: xhr.status,
            responseTime
          },
          void 0,
          // clientDetails
          errorDetails
        );
        xhr.removeEventListener("loadend", onLoadEnd);
      };
      xhr.addEventListener("loadend", onLoadEnd);
      self.originalXhrSend.apply(this, arguments);
    };
  }
  static init() {
    if (this.isInitialized) return;
    try {
      this.patchFetch();
      this.patchXMLHttpRequest();
      this.isInitialized = true;
    } catch (error) {
      console.error("[CHO:LOG] SDK: NetworkInterceptor \uCD08\uAE30\uD654\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.", error);
    }
  }
};

// src/core/errorCatcher.ts
var ErrorCatcher = class {
  static {
    this.isInitialized = false;
  }
  static {
    // handleGlobalError 및 handleUnhandledRejection 메서드는 거의 동일하게 유지 가능
    // Logger.logError 호출 부분이 이미 새로운 시그니처와 잘 맞음
    this.handleGlobalError = (eventOrMessage, source, lineno, colno, errorObj) => {
      let logMessage = "\uCC98\uB9AC\uB418\uC9C0 \uC54A\uC740 \uC804\uC5ED \uC624\uB958";
      const errorDetails = { type: "UnknownError", message: "" };
      const clientDetails = {};
      let actualError = errorObj;
      if (eventOrMessage instanceof ErrorEvent && eventOrMessage.error) {
        actualError = eventOrMessage.error;
        logMessage = eventOrMessage.message || actualError?.message || "\uC624\uB958 \uBA54\uC2DC\uC9C0\uB97C \uC0AC\uC6A9\uD560 \uC218 \uC5C6\uC74C";
        errorDetails.type = actualError?.name || "ErrorEvent";
        errorDetails.message = actualError?.message || logMessage;
        if (actualError?.stack) errorDetails.stacktrace = actualError.stack;
      } else if (typeof eventOrMessage === "string") {
        logMessage = eventOrMessage;
        errorDetails.message = eventOrMessage;
        if (actualError) {
          errorDetails.type = actualError.name;
          if (actualError.stack) errorDetails.stacktrace = actualError.stack;
        } else {
          errorDetails.type = "GenericError";
          if (source) errorDetails.message += ` in ${source}`;
        }
      } else if (actualError) {
        logMessage = actualError.message;
        errorDetails.type = actualError.name;
        errorDetails.message = actualError.message;
        if (actualError.stack) errorDetails.stacktrace = actualError.stack;
      }
      if (errorDetails.stacktrace?.includes("cholog") || logMessage?.includes("Cholog SDK")) {
        console.warn("[CHO:LOG] SDK \uB0B4\uBD80\uC5D0\uC11C \uBC1C\uC0DD\uD55C \uC7A0\uC7AC\uC801 \uC7AC\uADC0 \uC624\uB958\uB97C \uCC28\uB2E8\uD588\uC2B5\uB2C8\uB2E4.", logMessage);
        return;
      }
      if (typeof window !== "undefined") {
        clientDetails.url = window.location.href;
      }
      Logger.logError(logMessage, errorDetails, clientDetails);
    };
  }
  static {
    this.handleUnhandledRejection = (event) => {
      let reason = event.reason;
      let logMessage = "\uCC98\uB9AC\uB418\uC9C0 \uC54A\uC740 \uD504\uB85C\uBBF8\uC2A4 \uAC70\uBD80";
      const errorDetails = { type: "UnhandledRejection", message: "" };
      const clientDetails = {};
      if (reason instanceof Error) {
        logMessage = reason.message || "\uD504\uB85C\uBBF8\uC2A4\uAC00 \uC624\uB958\uC640 \uD568\uAED8 \uAC70\uBD80\uB428";
        errorDetails.type = reason.name || "UnhandledRejectionError";
        errorDetails.message = reason.message;
        if (reason.stack) errorDetails.stacktrace = reason.stack;
      } else {
        try {
          errorDetails.message = `\uC6D0\uC778: ${JSON.stringify(reason)}`;
        } catch {
          errorDetails.message = `\uC6D0\uC778: [\uC9C1\uB82C\uD654 \uBD88\uAC00\uB2A5]`;
        }
        logMessage = `Unhandled promise rejection: ${errorDetails.message}`;
      }
      if (errorDetails.stacktrace?.includes("cholog") || logMessage?.includes("Cholog SDK")) {
        console.warn("[CHO:LOG] SDK \uB0B4\uBD80\uC5D0\uC11C \uBC1C\uC0DD\uD55C \uC7A0\uC7AC\uC801 \uC7AC\uADC0 \uC624\uB958\uB97C \uCC28\uB2E8\uD588\uC2B5\uB2C8\uB2E4.", logMessage);
        return;
      }
      if (typeof window !== "undefined") {
        clientDetails.url = window.location.href;
      }
      Logger.logError(logMessage, errorDetails, clientDetails);
    };
  }
  static init() {
    if (this.isInitialized || typeof window === "undefined") {
      return;
    }
    try {
      window.onerror = this.handleGlobalError;
      window.addEventListener("unhandledrejection", this.handleUnhandledRejection);
      this.isInitialized = true;
    } catch (error) {
      console.error("[CHO:LOG] SDK: ErrorCatcher \uCD08\uAE30\uD654\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.", error);
    }
  }
};

// src/core/eventTracker.ts
var EventTracker = class {
  static {
    this.config = {
      significantElementSelector: 'button, a, [role="button"], input[type="submit"], [data-cholog-action]'
    };
  }
  static init(options) {
    if (typeof window === "undefined") return;
    if (options?.significantElementSelector) {
      this.config.significantElementSelector = options.significantElementSelector;
    }
    this.logNavigation(window.location.href, "initial_load");
    window.addEventListener("hashchange", () => this.logNavigation(window.location.href, "hash_change"));
    window.addEventListener("popstate", () => this.logNavigation(window.location.href, "popstate_navigation"));
    document.addEventListener(
      "click",
      (event) => {
        const targetElement = event.target;
        const closestSignificantElement = targetElement.closest(this.config.significantElementSelector);
        if (closestSignificantElement) {
          RequestContext.startNewRequest();
          const eventDetails = {
            type: "user_interaction_click",
            // 또는 "significant_click"
            targetSelector: this.getElementPath(closestSignificantElement),
            properties: {
              // textContent는 개인정보 포함 가능성 있어 주의
              // elementText: closestSignificantElement.textContent?.trim().substring(0, 50) || "",
              elementType: closestSignificantElement.tagName.toLowerCase(),
              elementId: closestSignificantElement.id || void 0,
              elementClasses: closestSignificantElement.className || void 0
            }
          };
          Logger.logEvent(`\uD074\uB9AD \uC774\uBCA4\uD2B8 => ${eventDetails.targetSelector}`, eventDetails);
        }
      },
      true
      // Use capture phase
    );
  }
  static logNavigation(url, navigationType) {
    RequestContext.startNewRequest();
    const eventDetails = {
      type: navigationType,
      // 예: "initial_load", "spa_navigation"
      properties: { currentUrl: url }
    };
    Logger.logEvent(`\uB124\uBE44\uAC8C\uC774\uC158 \uC774\uBCA4\uD2B8 => ${navigationType} to ${url}`, eventDetails);
  }
  static getElementPath(element) {
    const parts = [];
    let currentElement = element;
    while (currentElement && currentElement.tagName) {
      let selector = currentElement.tagName.toLowerCase();
      if (currentElement.id) {
        selector += `#${currentElement.id}`;
        parts.unshift(selector);
        break;
      } else if (currentElement.classList && currentElement.classList.length > 0) {
        selector += `.${Array.from(currentElement.classList).join(".")}`;
      }
      parts.unshift(selector);
      if (currentElement === document.body || parts.length >= 7) break;
      currentElement = currentElement.parentElement;
    }
    return parts.join(" > ");
  }
};

// src/index.ts
var Cholog = {
  init: (config) => {
    RequestContext.startNewRequest();
    const {
      apiKey,
      environment,
      enableEventTracker = true,
      // 기본값 true
      enableErrorCatcher = true,
      // 기본값 true
      enableNetworkInterceptor = true,
      // 기본값 true
      loggerOptions
    } = config;
    Logger.init({
      apiKey,
      environment,
      ...loggerOptions || {}
      // loggerOptions가 있으면 해당 값 사용
    });
    if (enableNetworkInterceptor) {
      NetworkInterceptor.init();
    }
    if (enableErrorCatcher) {
      ErrorCatcher.init();
    }
    if (enableEventTracker) {
      EventTracker.init();
    }
    const loggedEnvironment = environment || "default (by Logger)";
    Logger.info("Cholog SDK Initialized", {
      sdk: "cholog-sdk",
      version: "1.0.7",
      // SDK 버전에 맞게 수정하세요.
      features: {
        eventTracker: enableEventTracker,
        errorCatcher: enableErrorCatcher,
        networkInterceptor: enableNetworkInterceptor
      },
      configApplied: {
        projectKeyObfuscated: apiKey ? `${apiKey.substring(0, 3)}...` : "NOT_SET",
        // 실제 키 일부만 표시
        environment: loggedEnvironment,
        batchInterval: loggerOptions?.batchInterval || "default (Logger)",
        maxQueueSize: loggerOptions?.maxQueueSize || "default (Logger)"
      }
    });
  },
  // Logger의 자체 로깅 메서드들을 직접 노출
  // payload는 선택적 인자
  log: (message, payload) => Logger.log(message, payload),
  info: (message, payload) => Logger.info(message, payload),
  warn: (message, payload) => Logger.warn(message, payload),
  error: (message, payload) => {
    Logger.error(message, payload);
  },
  debug: (message, payload) => Logger.debug(message, payload),
  trace: (message, payload) => Logger.trace(message, payload)
  // 필요하다면 TraceContext의 메서드도 일부 노출 가능
  // startNewTrace: () => TraceContext.startNewTrace(),
  // getCurrentTraceId: () => TraceContext.getCurrentTraceId(),
};
var index_default = Cholog;
export {
  Cholog,
  index_default as default
};
