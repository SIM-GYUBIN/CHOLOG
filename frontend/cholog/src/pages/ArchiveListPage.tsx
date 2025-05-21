import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import EachLog from "../components/eachLog";
import ProjectNavBar from "../components/projectNavbar";
import { useDispatch, useSelector } from "react-redux";
import { fetchArchivedLogs } from "../store/slices/logSlice";
import { RootState, AppDispatch } from "../store/store";
import { fetchProjectDetail } from "../store/slices/projectSlice";
import { motion } from "framer-motion";

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

export default function ArchiveListPage() {
  const { projectId } = useParams();
  const dispatch = useDispatch<AppDispatch>();
  const { archivedLogs, isLoading, error } = useSelector(
    (state: RootState) => state.log
  );
  const { projects } = useSelector((state: RootState) => state.project);
  const [expandedReasons, setExpandedReasons] = useState<
    Record<string, boolean>
  >({});

  useEffect(() => {
    if (projectId) {
      dispatch(fetchArchivedLogs({ projectId, page: 1, size: 10 }));
      dispatch(fetchProjectDetail(Number(projectId)));
    }
  }, [projectId, dispatch]);

  const currentProject = projects.find((p) => p.id === Number(projectId));
  const handlePageChange = (newPage: number) => {
    if (projectId) {
      dispatch(fetchArchivedLogs({ projectId, page: newPage, size: 10 }));
    }
  };

  const toggleReason = (logId: string) => {
    setExpandedReasons((prev) => ({ ...prev, [logId]: !prev[logId] }));
  };

  const pagination = archivedLogs || {
    pageNumber: 1,
    totalPages: 1,
    totalElements: 0,
    pageSize: 10,
    first: true,
    last: true,
  };

  const archiveLogs = archivedLogs?.content || [];

  return (
    <motion.div
      className="max-w-[65vw] mx-auto"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      <ProjectNavBar />

      <motion.div
        className="flex flex-row justify-between mb-4"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.2 }}
      >
        <div className="flex flex-row items-center gap-2 font-[paperlogy5]">
          <div className="text-[24px] text-slate-400">
            {currentProject?.name || "프로젝트를 찾을 수 없습니다"}
          </div>
        </div>
      </motion.div>

      {isLoading ? (
        <motion.div
          className="border rounded-xl border-[var(--line)] bg-white/5"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
        >
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
        </motion.div>
      ) : error ? (
        <motion.div
          className="flex flex-col items-center justify-center h-48 bg-white/5 rounded-xl border border-[var(--line)]"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
        >
          <div className="text-xl text-red-500 mb-2">오류가 발생했습니다</div>
          <div className="text-gray-500">{error.message}</div>
        </motion.div>
      ) : (
        <>
          <motion.div
            className="border rounded-xl border-[var(--line)] bg-white/5"
            initial="hidden"
            animate="visible"
            variants={{
              hidden: { opacity: 0 },
              visible: {
                opacity: 1,
                transition: { staggerChildren: 0.08 },
              },
            }}
          >
            {archiveLogs.map((log, index) => (
              <motion.div
                key={log.logId}
                variants={{
                  hidden: { opacity: 0, y: 10 },
                  visible: { opacity: 1, y: 0 },
                }}
              >
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
                <div className="px-6 py-3 text-[14px] text-[var(--helpertext)] border-t border-[var(--line)]/10">
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
                          className="text-[12px] text-lime-600 mt-1 hover:underline cursor-pointer"
                        >
                          {expandedReasons[log.logId] ? "접기" : "더보기"}
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </motion.div>
            ))}
          </motion.div>

          {archiveLogs.length > 0 && (
            <motion.div
              className="flex justify-center text-center gap-2 mt-6 text-[12px]"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.2 }}
            >
              <button
                onClick={() => handlePageChange(pagination.pageNumber - 1)}
                disabled={pagination.first}
                className={`px-3 py-1 rounded-xl ${
                  pagination.first
                    ? "bg-[var(--bg))] border border-[var(--line)] text-slate-400"
                    : "text-[var(--text)] hover:bg-[var(--bg)]"
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
                        ? "bg-lime-500 text-white"
                        : "bg-[var(--bg)] text-[var(--text)] hover:bg-slate-400/20"
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
                    ? "bg-[var(--bg)] border border-[var(--line)] text-slate-400"
                    : "bg-[var(--bg)] text-[var(--text)] hover:bg-slate-400/20"
                }`}
              >
                다음
              </button>
            </motion.div>
          )}

          {archiveLogs.length === 0 && (
            <motion.div
              className="flex flex-col items-center justify-center h-48 bg-white/5 rounded-2xl border border-[var(--line)]"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
            >
              <div className="text-lg sm:text-xl text-[#5EA500] mb-2">
                아카이브된 로그가 없습니다
              </div>
              <div className="text-sm sm:text-base text-[var(--helpertext)]">
                아직 아카이브된 로그가 없습니다.
              </div>
            </motion.div>
          )}
        </>
      )}
    </motion.div>
  );
}
