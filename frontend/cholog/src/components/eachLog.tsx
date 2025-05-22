import { useNavigate } from "react-router-dom";

type LogDetails = {
  errorCode?: string;
  stackTrace?: string;
};

type LogProps = {
  id: string;
  timestamp: string;
  level: string;
  message: string;
  source: "frontend" | "backend";
  environment: string;
  islevelBg?: boolean;
};

export default function EachLog({
  id,
  timestamp,
  level,
  message,
  source,
  environment,
  islevelBg,
}: LogProps) {
  const formattedTime = timestamp
    ? (() => {
        const date = new Date(timestamp);
        const MM = String(date.getMonth() + 1).padStart(2, "0");
        const dd = String(date.getDate()).padStart(2, "0");
        const HH = String(date.getHours()).padStart(2, "0");
        const mm = String(date.getMinutes()).padStart(2, "0");
        const ss = String(date.getSeconds()).padStart(2, "0");
        return `${MM}-${dd} ${HH}:${mm}:${ss}`;
      })()
    : "";

  const nav = useNavigate();

  const handleclick = () => {
    if (id) {
      // URL에서 projectId 추출
      const pathParts = window.location.pathname.split("/");
      const projectIdIndex =
        pathParts.findIndex((part) => part === "project") + 1;
      const projectId =
        projectIdIndex > 0 && projectIdIndex < pathParts.length
          ? pathParts[projectIdIndex]
          : null;

      if (projectId) {
        nav(`/project/${projectId}/log/${id}`);
      } else {
        // 프로젝트 ID를 찾을 수 없는 경우 처리
        console.error("프로젝트 ID를 찾을 수 없습니다.");
      }
    }
  };

  const levelBg =
    {
      FATAL: "bg-[rgba(128,0,128,0.04)]", // 보라색 6%
      ERROR: "bg-[rgba(239,68,68,0.04)]", // 빨간색 6%
      WARN: "bg-[rgba(234,179,8,0.04)]", // 노란색 6%
      INFO: "bg-[rgba(59,130,246,0.04)]", // 파란색 6%
      DEBUG: "bg-[rgba(34,197,94,0.04)]", // 연두색 6%
      TRACE: "bg-[rgba(20,184,166,0.04)]", // 에메랄드색 6%
    }[level] || "bg-[rgba(255,255,255,0.04)]";

  const levelCircle =
    {
      FATAL: "bg-purple-500",
      ERROR: "bg-red-500",
      WARN: "bg-yellow-500",
      INFO: "bg-blue-500",
      DEBUG: "bg-green-500",
      TRACE: "bg-teal-500",
    }[level] || "bg-white";

  // islevelBg가 true면 levelBg 적용, 아니면 배경색 없음
  const containerClass = `border-b border-b-[1.5px] border-b-[var(--helpertext)] px-4 py-3 cursor-pointer hover:bg-white/15 transition-shadow ${islevelBg ? levelBg : ""}`;

  return (
    <div className={containerClass} onClick={handleclick}>
      <div className="grid grid-cols-8 text-[14px] text-[var(--text)]">
        {/* 로그 레벨 */}
        <div className="col-span-1 flex flex-row justify-start items-center shrink-0 w-20 gap-2">
          <div className={`${levelCircle} w-4 h-4 rounded-full`}></div>
          <div className="font-semibold truncate">{level}</div>
        </div>

        {/* 나머지 */}
        <div className="flex flex-row items-start col-span-7 gap-10">
          <div className="items-center grid grid-cols-10 gap-10 w-full">
            <div className="col-span-2 shrink-0 min-w-0 truncate">{source}</div>
            <div className="col-span-2 shrink-0 min-w-0 truncate">
              {environment}
            </div>
            <div className="col-span-3 text-start min-w-0 truncate">
              {message}
            </div>
            <div className="col-span-3 min-w-0 shrink-0 text-start">
              {formattedTime}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
