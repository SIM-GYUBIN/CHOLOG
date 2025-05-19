import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import EachLog from "../components/eachLog";
import ProjectNavBar from "../components/projectNavbar";
import { useDispatch, useSelector } from "react-redux";
import { fetchArchivedLogs } from "../store/slices/logSlice";
import { RootState, AppDispatch } from "../store/store";

interface ArchiveLog {
  logId: string;
  nickname: string;
  memo: string;
  logLevel: "TRACE" | "DEBUG" | "INFO" | "WARN" | "ERROR" | "FATAL";
  logSource: "frontend" | "backend";
  logType: string;
  logEnvironment: string;
  logMessage: string;
  logTimestamp: string;
}

interface PaginationInfo {
  pageNumber: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  first: boolean;
  last: boolean;
}

interface ApiResponse {
  success: boolean;
  data: {
    content: ArchiveLog[];
    pageNumber: number;
    totalPages: number;
    totalElements: number;
    pageSize: number;
    first: boolean;
    last: boolean;
  };
  timestamp: string;
}

export default function ArchiveListPage() {
  const { projectId } = useParams();
  const dispatch = useDispatch<AppDispatch>();
  const { archivedLogs, isLoading, error } = useSelector(
    (state: RootState) => state.log
  );
  const { projects } = useSelector((state: RootState) => state.project);
  const [expandedReasons, setExpandedReasons] = useState<Record<string, boolean>>({});

  useEffect(() => {
    if (projectId) {
      dispatch(fetchArchivedLogs({ projectId, page: 1, size: 10 }));
    }
  }, [projectId, dispatch]);

  // 현재 프로젝트 찾기
  const currentProject = projects.find(p => p.id === Number(projectId));

  const handlePageChange = (newPage: number) => {
    if (projectId) {
      dispatch(fetchArchivedLogs({ projectId, page: newPage, size: 10 }));
    }
  };

  const toggleReason = (logId: string) => {
    setExpandedReasons((prev) => ({
      ...prev,
      [logId]: !prev[logId],
    }));
  };

  // 페이지네이션 정보 추출
  const pagination = archivedLogs || {
    pageNumber: 1,
    totalPages: 1,
    totalElements: 0,
    pageSize: 10,
    first: true,
    last: true,
  };

  // 아카이븴 로그 컨텐츠 추출
  const archiveLogs = archivedLogs?.content || [];

  return (
    <div className="max-w-[60vw] mx-auto">
      <ProjectNavBar />

      <div className="flex flex-row justify-between mb-4">
        <div className="flex flex-row items-center gap-2 font-[paperlogy5]">
          <div className="text-[24px] text-slate-500">
            {currentProject?.name || "프로젝트를 찾을 수 없습니다"}
          </div>
        </div>
      </div>

      {isLoading ? (
        <div className="border rounded-xl border-[var(--line)] bg-white/5">
          {[...Array(3)].map((_, index) => (
            <div key={index} className="animate-pulse">
              <div
                className={`p-4 ${index !== 0 ? "border-t border-[var(--line)]" : ""}`}
              >
                <div className="h-6 bg-slate-200 rounded w-1/4 mb-2"></div>
                <div className="h-4 bg-slate-200 rounded w-3/4"></div>
              </div>
              <div className="px-8 pb-4">
                <div className="h-4 bg-slate-200 rounded w-2/3"></div>
              </div>
            </div>
          ))}
        </div>
      ) : error ? (
        <div className="flex flex-col items-center justify-center h-48 bg-white/5 rounded-xl border border-[var(--line)]">
          <div className="text-xl text-red-500 mb-2">오류가 발생했습니다</div>
          <div className="text-gray-500">{error.message}</div>
        </div>
      ) : (
        <>
          <div className="border rounded-xl border-[var(--line)] bg-white/5">
            {archiveLogs.map((log, index) => (
              <div key={log.logId} className="w-full">
                <div
                  className={`flex w-full ${index !== 0 ? "border-t border-[var(--line)]" : ""}`}
                >
                  <div className="w-full py-2 px-4">
                    <EachLog
                      id={log.logId}
                      timestamp={new Date(log.logTimestamp)
                        .toLocaleString("ko-KR", {
                          month: "2-digit",
                          day: "2-digit",
                          hour: "2-digit",
                          minute: "2-digit",
                          hour12: false,
                        })
                        .replace(/\./g, "-")}
                      level={log.logLevel}
                      message={log.logMessage}
                      environment={log.logEnvironment}
                      source={log.logSource}
                      islevelBg={false}
                    />
                  </div>
                </div>
                <div className="px-6 py-3 text-[14px] text-slate-600 border-t border-[var(--line)]/10">
                  <div className="text-start">
                    <div
                      className={`${!expandedReasons[log.logId] ? "line-clamp-2" : ""} break-keep font-[paperlogy4] tracking-wider`}
                      onClick={() => toggleReason(log.logId)}
                    >
                      {log.memo}
                    </div>
                    <div className="flex justify-end">
                      {log.memo.length > 100 && (
                        <button
                          onClick={() => toggleReason(log.logId)}
                          className="text-[12px] text-green-600 mt-1 hover:underline cursor-pointer"
                        >
                          {expandedReasons[log.logId] ? "접기" : "더보기"}
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {archiveLogs.length > 0 && (
            <div className="flex justify-center text-center gap-2 mt-6 text-[12px]">
              <button
                onClick={() => handlePageChange(pagination.pageNumber - 1)}
                disabled={pagination.first}
                className={`px-3 py-1 rounded-xl ${
                  pagination.first
                    ? "bg-slate-100 text-slate-400"
                    : "text-slate-600 hover:bg-slate-200"
                }`}
              >
                이전
              </button>

              <div className="flex gap-1">
                {Array.from(
                  { length: pagination.totalPages },
                  (_, i) => i + 1
                ).map((page) => (
                  <button
                    key={page}
                    onClick={() => handlePageChange(page)}
                    className={`px-3 py-1 rounded-xl ${
                      page === pagination.pageNumber
                        ? "bg-[rgba(101,218,94,1)] text-white"
                        : "bg-white text-slate-600 hover:bg-slate-200"
                    }`}
                  >
                    {page}
                  </button>
                ))}
              </div>

              <button
                onClick={() => handlePageChange(pagination.pageNumber + 1)}
                disabled={pagination.last}
                className={`px-3 py-1 rounded-xl ${
                  pagination.last
                    ? "bg-slate-100 text-slate-400"
                    : "bg-white text-slate-600 hover:bg-slate-200"
                }`}
              >
                다음
              </button>
            </div>
          )}

          {archiveLogs.length === 0 && (
            <div className="flex flex-col items-center justify-center h-48 bg-white/5 rounded-2xl border border-[var(--line)]">
              <div className="text-lg sm:text-xl text-[#5EA500] mb-2">아카이브된 로그가 없습니다</div>
              <div className="text-sm sm:text-base text-gray-500">아직 아카이브된 로그가 없습니다.</div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
