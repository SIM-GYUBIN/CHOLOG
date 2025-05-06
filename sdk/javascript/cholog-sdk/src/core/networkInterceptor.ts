// src/core/networkInterceptor.ts
import { TraceContext } from "./traceContext";
import { Logger } from "./logger";
import { LogHttp, LogHttpRequest, LogHttpResponse, LogError } from "../types";

export class NetworkInterceptor {
  private static isInitialized = false;
  private static originalFetch: typeof window.fetch | null = null;
  private static originalXhrSend: typeof XMLHttpRequest.prototype.send | null = null;

  private static patchFetch(): void {
    this.originalFetch = window.fetch;
    const self = this; // Logger.logHttp 호출 시 this 컨텍스트 불필요

    window.fetch = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
      const requestUrlStr = typeof input === "string" ? input : (input as URL).toString();
      if (requestUrlStr.startsWith(Logger.getApiEndpoint())) {
        return NetworkInterceptor.originalFetch!.call(window, input, init);
      }

      let traceId = TraceContext.getCurrentTraceId();
      if (!traceId) traceId = TraceContext.startNewTrace();

      const modifiedInit = { ...(init || {}) };
      modifiedInit.headers = new Headers(modifiedInit.headers); // 헤더 복사 및 Headers 객체로 통일
      (modifiedInit.headers as Headers).set("X-Request-ID", traceId);

      const startTime = Date.now();
      const requestDetails: LogHttpRequest = {
        method: (modifiedInit.method || "GET").toUpperCase(),
        url: requestUrlStr,
      };

      Logger.logHttp(`API Request START: ${requestDetails.method} ${requestDetails.url}`, { request: requestDetails });

      try {
        const response = await self.originalFetch!.call(window, input, modifiedInit);
        const duration = Date.now() - startTime;
        const responseDetails: LogHttpResponse = { statusCode: response.status };
        Logger.logHttp(`API Request END: ${response.status} ${response.url}`, {
          request: requestDetails,
          response: responseDetails,
          durationMs: duration,
        });
        return response;
      } catch (error: any) {
        const duration = Date.now() - startTime;
        const errorDetails: LogError = {
          type: error?.name || "FetchError",
          message: error?.message || "Network request failed",
          stacktrace: error?.stack,
        };
        Logger.logHttp(
          `API Request FAILED: ${requestDetails.method} ${requestDetails.url}`,
          { request: requestDetails, durationMs: duration },
          undefined, // clientDetails는 Logger가 채움
          errorDetails
        );
        throw error;
      }
    };
  }

  private static patchXMLHttpRequest(): void {
    this.originalXhrSend = XMLHttpRequest.prototype.send;
    const originalXhrOpen = XMLHttpRequest.prototype.open;

    XMLHttpRequest.prototype.open = function (
      method: string,
      url: string | URL
      // ... other args
    ): void {
      (this as any)._chologMethod = method;
      (this as any)._chologUrl = typeof url === "string" ? url : url.toString();
      originalXhrOpen.apply(this, arguments as any);
    };

    XMLHttpRequest.prototype.send = function (body?: Document | XMLHttpRequestBodyInit | null) {
      const xhr = this as XMLHttpRequest & {
        _chologMethod?: string;
        _chologUrl?: string;
        _chologStartTime?: number;
        _chologTraceId?: string;
      };
      const requestUrlStr = xhr._chologUrl;

      if (requestUrlStr && requestUrlStr.startsWith(Logger.getApiEndpoint())) {
        return NetworkInterceptor.originalXhrSend!.apply(this, arguments as any);
      }

      let traceId = TraceContext.getCurrentTraceId();
      if (!traceId) traceId = TraceContext.startNewTrace();
      xhr._chologTraceId = traceId;
      this.setRequestHeader("X-Request-ID", traceId);

      const requestDetails: LogHttpRequest = {
        method: (xhr._chologMethod || "UnknownMethod").toUpperCase(),
        url: xhr._chologUrl || "UnknownURL",
      };

      const handleLoadEnd = () => {
        // load, error, abort, timeout에서 호출됨
        if (!(xhr as any)._chologLogged) {
          // 한번만 로깅되도록
          (xhr as any)._chologLogged = true;
          const duration = xhr._chologStartTime ? Date.now() - xhr._chologStartTime : undefined;
          const responseDetails: LogHttpResponse = { statusCode: xhr.status };
          let errorDetails: LogError | undefined = undefined;

          if (xhr.status === 0 || xhr.status >= 400) {
            // 0은 네트워크 에러 등
            errorDetails = {
              type: xhr.statusText || "XHRError",
              message: `XHR request failed with status ${xhr.status}`,
            };
          }
          Logger.logHttp(
            `API Request ${errorDetails ? "FAILED" : "END"}: ${xhr.status} ${xhr.responseURL || requestDetails.url}`,
            { request: requestDetails, response: responseDetails, durationMs: duration },
            undefined,
            errorDetails
          );
        }
        // 리스너 정리
        xhr.removeEventListener("load", handleLoadEnd);
        xhr.removeEventListener("error", handleLoadEnd);
        xhr.removeEventListener("abort", handleLoadEnd);
        xhr.removeEventListener("timeout", handleLoadEnd);
      };

      xhr.addEventListener("loadstart", () => {
        xhr._chologStartTime = Date.now();
        Logger.logHttp(`API Request START: ${requestDetails.method} ${requestDetails.url}`, {
          request: requestDetails,
        });
      });

      xhr.addEventListener("load", handleLoadEnd);
      xhr.addEventListener("error", handleLoadEnd);
      xhr.addEventListener("abort", handleLoadEnd);
      xhr.addEventListener("timeout", handleLoadEnd);

      return NetworkInterceptor.originalXhrSend!.apply(this, arguments as any);
    };
  }

  public static init(): void {
    if (this.isInitialized || typeof window === "undefined") return;
    try {
      this.patchFetch();
      this.patchXMLHttpRequest();
      this.isInitialized = true;
    } catch (error) {
      console.error("Cholog SDK: Failed to initialize NetworkInterceptor.", error);
    }
  }
}
