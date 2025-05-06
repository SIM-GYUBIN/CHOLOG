// src/index.ts
import { Logger } from "./core/logger";
import { NetworkInterceptor } from "./core/networkInterceptor";
import { ErrorCatcher } from "./core/errorCatcher";
import { EventTracker } from "./core/eventTracker";
import { TraceContext } from "./core/traceContext";

export const Cholog = {
  init: (config: { appKey: string }) => {
    TraceContext.startNewTrace();
    Logger.init(config);
    NetworkInterceptor.init();
    ErrorCatcher.init();
    EventTracker.init();
    Logger.info("Cholog SDK Initialized", {
      traceId: TraceContext.getCurrentTraceId(),
    });
  },

  log: Logger.info.bind(Logger),
  info: Logger.info.bind(Logger),
  warn: Logger.warn.bind(Logger),
  error: Logger.error.bind(Logger),
  debug: Logger.debug.bind(Logger),
  trace: Logger.trace.bind(Logger),
};

export default Cholog;
