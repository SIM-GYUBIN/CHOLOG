import { Logger } from "./core/logger";
import { NetworkInterceptor } from "./core/networkInterceptor";
import { ErrorCatcher } from "./core/errorCatcher";
import { EventTracker } from "./core/eventTracker";

export const ChologSDK = {
  init: (config: { appKey: string }) => {
    Logger.init(config);
    NetworkInterceptor.init(config);
    ErrorCatcher.init();
    EventTracker.init();
  },
};

export default ChologSDK;

// window에 붙이는 것은 UMD 빌드로 자동 처리됨
