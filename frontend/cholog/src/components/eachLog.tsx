import errorIcon from "@/assets/levelicon/error.png";
import warnIcon from "@/assets/levelicon/warn.png";
import infoIcon from "@/assets/levelicon/info.png";
import debugIcon from "@/assets/levelicon/debug.png";
import fatalIcon from "@/assets/levelicon/fatal.png";
import traceIcon from "@/assets/levelicon/trace.png";

type LogDetails = {
  errorCode?: string;
  stackTrace?: string;
};

type LogProps = {
  islevelBg?: boolean; // Added islevelBg prop for conditional styling
  id?: string;
  from?: string;
  timestamp: string;
  message?: string;
  apiPath?: string;
  level: "INFO" | "WARN" | "ERROR" | "DEBUG" | "FATAL" | "TRACE";
  traceId?: string;
  spanId?: string;
  details?: LogDetails; // Changed from object to LogDetails for better type safety
};

const getLevelIcon = (level: string) => {
  switch (level) {
    case "ERROR":
      return errorIcon;
    case "WARN":
      return warnIcon;
    case "INFO":
      return infoIcon;
    case "DEBUG":
      return debugIcon;
    case "FATAL":
      return fatalIcon;
    case "TRACE":
      return traceIcon;
    default:
      return infoIcon;
  }
};

export default function EachLog({
  timestamp,
  message,
  from,
  apiPath,
  level,
  // traceId,
  // spanId,
  // details,
  islevelBg = false,
}: LogProps) {
  const formattedTime = timestamp
  ? new Date(timestamp).toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false, // 24시간 형식으로 표시
    }).replace(/\/|, /g, '-').replace(' ', ' ') // 날짜 구분자를 변경하고, 공백으로 시간 구분
  : "";
  // const formattedTime = timestamp ? new Date(timestamp).toLocaleString() : "";
  const levelBg = {
    FATAL: "bg-[rgba(128,0,128,0.06)]",   // purple-100 6%
    ERROR: "bg-[rgba(248,113,113,0.06)]", // red-100 6%
    WARN: "bg-[rgba(253,224,71,0.06)]",   // yellow-100 6%
    INFO: "bg-[rgba(191,219,254,0.06)]",  // blue-100 6%
    DEBUG: "bg-[rgba(134,239,172,0.06)]", // g100 6%
    TRACE: "bg-[rgba(243,244,246,0.06)]", // gray-100 6%
  }[level] || "bg-[rgba(255,255,255,0.06)]";

  // islevelBg가 true면 levelBg 적용, 아니면 배경색 없음
  const containerClass = `border-b border-b-[1.5px] border-b-slate-200 px-4 py-2 hover:shadow-lg transition-shadow ${islevelBg ? levelBg : ""}`;

  return (
    <div className={containerClass}>
      <div className="flex items-center text-[12px] text-slate-600 w-full">
        {/* 로그레벨아이콘+로그레벨: 15% */}
        <div className="flex flex-row items-center basis-[15%] shrink-0 grow-0 min-w-0 gap-2">
          <img
            src={getLevelIcon(level)}
            alt={`${level.toLowerCase()} 아이콘`}
            className="w-5 h-5"
          />
          <div className="font-semibold truncate">{level}</div>
        </div>
        {/* from: 10% */}
        <div className="basis-[10%] shrink-0 grow-0 min-w-0 truncate">
          {from}
        </div>
        {/* 메시지: 50% */}
        <div className="text-start basis-[50%] shrink-0 grow-0 min-w-0 truncate px-4">
          {message}
        </div>
        {/* 타임스탬프: 25% */}
        <div className="basis-[25%] shrink-0 grow-0 min-w-0">
          {formattedTime}
        </div>
      </div>
      <div className="mb-2">
        <div className="text-sm text-gray-600">{apiPath}</div>
      </div>
    </div>
  );
}
