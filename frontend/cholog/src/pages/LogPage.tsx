import { useNavigate, useParams } from "react-router-dom";
import { useState } from "react";
import EachLog from "../components/eachLog";
import ArchiveModal from "../components/ArchiveModal";
import ProjectNavBar from "../components/projectNavbar";

interface RelatedLog {
  type: "BE" | "FE";
  message: string;
  level: "error" | "warning" | "info" | "success";
}

import errorIcon from "@/assets/levelicon/error.svg";
import warnIcon from "@/assets/levelicon/warn.svg";
import infoIcon from "@/assets/levelicon/info.svg";
import debugIcon from "@/assets/levelicon/debug.svg";
import fatalIcon from "@/assets/levelicon/fatal.svg";
import traceIcon from "@/assets/levelicon/trace.svg";

const getLevelIcon = (level: string) => {
  switch (level.toUpperCase()) {
    case "ERROR":
      return errorIcon;
    case "WARNING":
      return warnIcon;
    case "INFO":
      return infoIcon;
    case "DEBUG":
      return debugIcon;
    case "FATAL":
      return fatalIcon;
    case "TRACE":
      return traceIcon;
    case "SUCCESS":
      return infoIcon;
    default:
      return infoIcon;
  }
};

const LogPage = () => {
  const { id } = useParams();
  const [isArchiveModalOpen, setIsArchiveModalOpen] = useState(false);

  console.log(id);

  // 임시 데이터
  const logData = {
    _id: "13df",
    type: "ERROR",
    timestamp: "2025-04-23 16:32:12",
    message: "API 호출 실패: Error: 500 Internal Server Error",
  };

  const mockLogs = [
    {
      _id: "trace-12345-span-67890",
      from: "FE",
      timestamp: "2025-04-28T12:00:00Z",
      message: "java.lang.NullPointerException at ...",
      level: "ERROR",
    },
    {
      _id: "trace-54321-span-09876",
      from: "BE",
      timestamp: "2025-04-28T11:58:00Z",
      message: "로그인 성공",
      level: "INFO",
    },
    {
      _id: "trace-98765-span-43210",
      from: "BE",
      timestamp: "2025-04-28T11:55:00Z",
      message: "Database connection established",
      level: "DEBUG",
    },
    {
      _id: "trace-24680-span-13579",
      from: "FE",
      timestamp: "2025-04-28T11:52:00Z",
      message: "Warning: Memory usage exceeds 80%",
      level: "WARN",
    },
    {
      _id: "trace-11111-span-22222",
      from: "BE",
      timestamp: "2025-04-28T11:50:00Z",
      message: "System crash detected",
      level: "FATAL",
    },
    {
      _id: "trace-33333-span-44444",
      from: "FE",
      timestamp: "2025-04-28T11:48:00Z",
      message: "API request completed",
      level: "TRACE",
    },
    {
      _id: "trace-55555-span-66666",
      from: "BE",
      timestamp: "2025-04-28T11:45:00Z",
      message: "User authentication successful",
      level: "INFO",
    },
    {
      _id: "trace-77777-span-88888",
      from: "FE",
      timestamp: "2025-04-28T11:42:00Z",
      message: "Component rendering error",
      level: "ERROR",
    },
    {
      _id: "trace-99999-span-00000",
      from: "BE",
      timestamp: "2025-04-28T11:40:00Z",
      message: "Cache cleared successfully",
      level: "DEBUG",
    },
    {
      _id: "trace-12121-span-34343",
      from: "FE",
      timestamp: "2025-04-28T11:38:00Z",
      message: "Network connection timeout",
      level: "WARN",
    },
  ];

  const nav = useNavigate();
  const handleclick = (id: string) => {
    if (id) {
      nav(`/log/${id}`);
    }
  };

  const handleArchive = (reason: string) => {
    console.log("아카이브 완료:", reason);
    setIsArchiveModalOpen(false);
    // 필요한 후속 처리
  };

  return (
    <div className="w-full lg:w-[80vw] mx-auto">
    <ProjectNavBar />

    <div className="flex gap-6 max-w-7xl mx-auto text-slate-600">

      {/* 메인 로그 섹션 */}
      <div className="flex-1 bg-white rounded-lg p-6 border border-[#E5E5E5]">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <img
              src="/src/assets/levelicon/error.svg"
              alt="error icon"
              className="w-11 h-11"
            />
            <div className="text-[28px] font-[paperlogy6]">{logData.type}</div>
          </div>
          <button
            onClick={() => setIsArchiveModalOpen(true)}
            className="p-2 rounded-full hover:bg-slate-100 transition-colors"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
          </button>
        </div>
        <div className="text-left font-[paperlogy4] mb-6">
          {logData.timestamp}
        </div>

        {/* 로그 메세지 섹션 */}
        <div className="mb-8">
          <div className="text-left p-4 text-[18px] font-[paperlogy6]">
            MESSAGE
          </div>
          <div className="text-left bg-[#F8FAFC] p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm">
            {logData.message}
          </div>
        </div>

        {/* 초록 LLM 섹션 */}
        <div className="mb-8">
          <div className="text-left p-4 text-[18px] font-[paperlogy6]">
            CHO:LOG EXPLANE
          </div>
          <div className="text-left bg-[#F7FEE7] p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm">
            {logData.message}
          </div>
        </div>
      </div>

      {/* 관련 로그 섹션 */}
      <div className="rounded-lg p-6 shadow-sm">
        <h2 className="text-left text-18px font-[paperlogy6] mb-6">
          Related Log
        </h2>
        <div className="space-y-4">
          {mockLogs.map((log, index) => (
            <div
              onClick={() => handleclick(log._id)}
              key={index}
              className="flex items-start gap-3 text-slate-600 cursor-pointer hover:bg-[#F7FEE7]"
            >
              <div className="relative flex items-center">
                <img
                  src={getLevelIcon(log.level)}
                  alt={`${log.level} icon`}
                  className="w-5 h-5 mt-1 ml-0.5"
                />
                <div
                  className="absolute h-10 w-[2px] bg-slate-200"
                  style={{ left: "50%", zIndex: -1 }}
                ></div>
              </div>
              <div className=" text-[14px]">{log.from}</div>
              <div className="text-[14px]">{log.message}</div>
            </div>
          ))}
        </div>
      </div>
      {/* 아카이브 모달 */}
      <ArchiveModal
        logId={logData._id} // id 파라미터 대신 현재 로그의 _id를 전달
        isOpen={isArchiveModalOpen}
        onClose={() => setIsArchiveModalOpen(false)}
        onArchive={handleArchive}
      />
    </div>
    </div>
  );
};

export default LogPage;
