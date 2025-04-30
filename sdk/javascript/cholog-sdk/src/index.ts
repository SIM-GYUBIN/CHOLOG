import { Logger } from "./core/logger";
import { NetworkInterceptor } from "./core/networkInterceptor";
import { ErrorCatcher } from "./core/errorCatcher";
import { EventTracker } from "./core/eventTracker";

export const Cholog = {
  init: (config: { appKey: string }) => {
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
  trace: Logger.trace.bind(Logger),
};

export default Cholog;

// window에 붙이는 것은 UMD 빌드로 자동 처리됨
