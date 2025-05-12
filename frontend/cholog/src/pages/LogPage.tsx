import { useNavigate, useParams } from "react-router-dom";
import { useState, useEffect } from "react";
import EachLog from "../components/eachLog";
import ArchiveModal from "../components/ArchiveModal";
import ProjectNavBar from "../components/projectNavbar";
import { useDispatch, useSelector } from "react-redux";
import frogimg from "@/assets/frog.png";

import { fetchLogDetail, fetchTraceLog } from "../store/slices/logSlice";
import { useAppDispatch } from "../hooks/useAppDispatch";

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
  const dispatch = useAppDispatch();
  const { logDetail, traceLogs, isLoading } = useSelector(
    (state: any) => state.log
  );

  useEffect(() => {
    if (id) {
      dispatch(fetchLogDetail(id));
    }
  }, [id]);

  useEffect(() => {
    if (logDetail?.traceId && logDetail?.projectId) {
      dispatch(
        fetchTraceLog({
          traceId: logDetail.traceId,
          projectId: logDetail.projectId,
        })
      );
    }
  }, [logDetail?.traceId]);

  // 임시 데이터
  const logData = logDetail ?? {
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

  const fallbackLogs =
    Array.isArray(traceLogs) && traceLogs.length > 0 ? traceLogs : mockLogs;

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

  const [isExplanationLoading, setIsExplanationLoading] = useState(false);
  const [showExplanation, setShowExplanation] = useState(false);

  const handleExplanationClick = () => {
    setIsExplanationLoading(true);
    setShowExplanation(false);
    
    setTimeout(() => {
      setIsExplanationLoading(false);
      setShowExplanation(true);
    }, 1000);
  };

  return (
    <div className="w-full lg:w-[80vw] mx-auto">
      <ProjectNavBar />

      <div className="flex gap-6 max-w-7xl mx-auto text-[var(--text)]">
        {/* 메인 로그 섹션 */}
        <div className="flex-1 bg-white/5 rounded-lg p-6 shadow-sm border border-[var(--line)]">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <img
                src={getLevelIcon(logData.type)}
                alt="level icon"
                className="w-11 h-11"
              />
              <div className="text-[28px] font-[paperlogy6]">
                {logData.type}
              </div>
            </div>
            <button
              onClick={() => setIsArchiveModalOpen(true)}
              className="p-2 rounded-full hover:bg-slate-100/50 transition-colors"
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
            <div className="text-left bg-slate-100/50 p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm">
              {logData.message}
            </div>
          </div>

          <div className="mb-8">
            <div className="text-left p-4 text-[18px] font-[paperlogy6]">
              CHO:LOG EXPLANE
            </div>
            <div
            className="cursor-pointer"
              onClick={handleExplanationClick}
            >
              {isExplanationLoading ? (
                <div className="flex gap-5 h-full px-6 py-3 text-[14px] shadow-sm hover:bg-lime-200/50 transition-all bg-[#F7FEE7] rounded-xl">
                  <div className="animate-spin rounded-full h-6 w-6 border-b-5 border-lime-600"></div>
                  <span>분석중...</span>
                </div>
              ) : showExplanation ? (
                <div className="text-[14px] text-left font-[consolaNormal] px-6 py-3 shadow-sm hover:bg-lime-200/50 transition-all bg-[#F7FEE7] rounded-lg">
                {logData.message}
                </div>
              ) : (
                <div className="flex justify-end gap-5">
                  <div className="h-full text-left px-6 py-3 text-[14px] shadow-sm hover:bg-lime-200/50 transition-all bg-[#F7FEE7] rounded-3xl">
                    <div>도움이 필요하면</div>
                    <div>나를 클릭하라굴~!</div>
                  </div>
                  <div className="w-20">
                      <img src={frogimg} alt="개구리" />
                    </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* 관련 로그 섹션 */}
        <div className="rounded-lg p-6 shadow-sm bg-white/5 border border-[var(--line)]">
          <h2 className="text-left text-18px font-[paperlogy6] mb-6">
            Related Log
          </h2>
          <div className="space-y-4">
            {!isLoading &&
              fallbackLogs.map((log, index) => (
                <div
                  onClick={() => handleclick(log._id)}
                  key={index}
                  className="flex items-start gap-3 text-[var(--text)] cursor-pointer hover:bg-[#F7FEE7]"
                >
                  <div className="relative flex items-center">
                    <img
                      src={getLevelIcon(log.level)}
                      alt={`${log.level} icon`}
                      className="w-5 h-5 mt-1 ml-0.5"
                    />
                    <div
                      className="absolute h-10 w-[2px] bg-[var(--helpertext)]"
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
