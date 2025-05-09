import errorIcon from "@/assets/levelicon/error.png";
import warnIcon from "@/assets/levelicon/warn.png";
import infoIcon from "@/assets/levelicon/info.png";
import debugIcon from "@/assets/levelicon/debug.png";
import fatalIcon from "@/assets/levelicon/fatal.png";
import traceIcon from "@/assets/levelicon/trace.png";
import { useNavigate } from "react-router-dom";

type LogDetails = {
  errorCode?: string;
  stackTrace?: string;
};

type LogProps = {
  islevelBg?: boolean; // Added islevelBg prop for conditional styling
  id?: string;
  from?: string;
  type?: string;
  status?:number; // http 상태코드
  timestamp?: string;
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
  id,
  timestamp,
  type,
  status,
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

  const nav = useNavigate();

  const handleclick = () => {
    if (id) {
      nav(`/log/${id}`);
    }
  };


  // const formattedTime = timestamp ? new Date(timestamp).toLocaleString() : "";
  const levelBg = {
    FATAL: "bg-[rgba(128,0,128,0.04)]",   // 보라색 6%
    ERROR: "bg-[rgba(239,68,68,0.04)]",   // 빨간색 6%
    WARN: "bg-[rgba(234,179,8,0.04)]",    // 노란색 6%
    INFO: "bg-[rgba(59,130,246,0.04)]",   // 파란색 6%
    DEBUG: "bg-[rgba(34,197,94,0.04)]",   // 연두색 6%
    TRACE: "bg-[rgba(20,184,166,0.04)]",  // 에메랄드색 6%
  }[level] || "bg-[rgba(255,255,255,0.04)]";

  const levelCircle = {
    FATAL: "bg-purple-500",
    ERROR: "bg-red-500",
    WARN: "bg-yellow-500",
    INFO: "bg-blue-500",
    DEBUG: "bg-green-500",
    TRACE: "bg-teal-500",
  }[level] || "bg-white";

  // islevelBg가 true면 levelBg 적용, 아니면 배경색 없음
  const containerClass = `border-b border-b-[1.5px] border-b-slate-200 px-4 py-2 cursor-pointer hover:shadow-[0_4px_12px_-2px_rgba(0,0,0,0.1),0_-4px_12px_-2px_rgba(0,0,0,0.1)] transition-shadow ${islevelBg ? levelBg : ""}`;
  
  return (
    <div className={containerClass} onClick={handleclick}>
      <div className="grid grid-cols-6 text-[12px] text-slate-600 w-full">

{/* 로그 레벨벨 */}
        <div className="col-span-1 flex flex-row items-center shrink-0 min-w-0 gap-2">
        <div className={`${levelCircle} w-4 h-4 rounded-full`}></div>
          <div className="font-semibold truncate">{level}</div>
        </div>

{/* 나머지지 */}
        <div className="flex flex-row items-start col-span-5 gap-10">
          
          <div className="items-center grid grid-cols-10 gap-10">
            <div className="col-span-0.5 shrink-0 min-w-0">
              {from}
            </div>
            <div className="col-span-1.5 shrink-0 min-w-0">
              {type}
            </div>
            <div className="col-span-1 shrink-0 min-w-0">
              {status}
            </div>
            <div className="col-span-4 text-start min-w-0 truncate ">
              {message}
            </div>
            <div className="col-span-3 min-w-0 shrink-0 ">
              {formattedTime}
            </div>
            
          </div>
        </div>
      </div>

    </div>
  );
}
