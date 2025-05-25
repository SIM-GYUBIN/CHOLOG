import React from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useState, useEffect } from "react";
import EachLog from "../components/eachLog";
import ArchiveModal from "../components/ArchiveModal";
import ProjectNavBar from "../components/projectNavbar";
import { useDispatch, useSelector } from "react-redux";
import { analyzeLLM } from "../store/slices/llmSlice";
import frogimg from "@/assets/frog.png";
import { LogDetail } from "../types/log.types";
import { fetchLogDetail, fetchTraceLog } from "../store/slices/logSlice";
import { useAppDispatch } from "../hooks/useAppDispatch";
import JiraMakingButton from "../components/JiraMakingButton";
import { motion } from "framer-motion";

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
    case "SUCCESS":
      return infoIcon;
    default:
      return infoIcon;
  }
};

// Animation variants
const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      duration: 0.3,
      ease: "easeOut",
      when: "beforeChildren",
      staggerChildren: 0.05,
    },
  },
};

const sectionVariants = {
  hidden: { opacity: 0, y: 10 },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.3,
      ease: "easeOut",
    },
  },
};

const itemVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      duration: 0.2,
      ease: "easeOut",
    },
  },
};

const LogPage = () => {
  const { projectId, logId } = useParams();
  const [isArchiveModalOpen, setIsArchiveModalOpen] = useState(false);
  const dispatch = useAppDispatch();
  const { logDetail, traceLogs, isLoading } = useSelector(
    (state: any) => state.log
  );

  // 스택트레이스 토글 상태 관리
  const [showStacktrace, setShowStacktrace] = useState(false);
  // 메시지 토글 상태 관리 추가
  const [showFullMessage, setShowFullMessage] = useState(false);

  useEffect(() => {
    if (logId && projectId) {
      dispatch(
        fetchLogDetail({
          logId,
          projectId: Number(projectId),
        })
      ).then((action) => {
        console.log("로그 디테일 API 응답:", action.payload);
      });
    }
  }, [logId, projectId, dispatch]);

  useEffect(() => {
    if (logDetail?.traceId && logDetail?.id) {
      dispatch(
        fetchTraceLog({
          traceId: logDetail.traceId,
          projectId: Number(projectId),
        })
      ).then((action) => {
        console.log("트레이스 로그 API 응답:", action.payload);
      });
    }
  }, [logDetail?.traceId, projectId, dispatch]);

  const nav = useNavigate();
  const handleclick = (id: string) => {
    if (id && projectId) {
      // LLM 분석 결과 초기화
      setShowExplanation(false);
      setShowFullMessage(false);
      setShowStacktrace(false);
      nav(`/project/${projectId}/log/${id}`);
    }
  };

  const handleArchive = (reason: string) => {
    console.log("아카이빙 완료:", reason);
    setIsArchiveModalOpen(false);
    // 아카이브 리스트 페이지로 리다이렉트
    nav(`/project/${projectId}/archives`);
  };

  const {
    result: explanation,
    isLoading: isExplanationLoading,
    error: explanationError,
  } = useSelector((state: any) => state.llm.analysis);
  const [showExplanation, setShowExplanation] = useState(false);

  const handleExplanationClick = async () => {
    if (!projectId || !logDetail?.id) return;

    setShowExplanation(true);
    dispatch(
      analyzeLLM({
        projectId,
        logId: logDetail.id,
      })
    );
  };

  // 객체를 JSON 형식으로 표시하는 함수
  const renderJsonObject = (obj: any) => {
    if (!obj) return null;
    return (
      <pre className="text-[12px] bg-[var(--sub)] p-2 rounded overflow-x-auto">
        {JSON.stringify(obj, null, 2)}
      </pre>
    );
  };

  return (
    <motion.div
      initial="hidden"
      animate="visible"
      variants={containerVariants}
      className="w-full max-w-[65vw] mx-auto px-4 lg:px-0"
    >
      <ProjectNavBar />

      <div className="flex flex-col lg:flex-row gap-6 mx-auto text-[var(--text)]">
        {/* 메인 로그 섹션 */}
        <motion.div
          variants={sectionVariants}
          className="flex-[2] min-w-0 bg-white/5 rounded-lg p-6 shadow-sm border border-[var(--line)]"
        >
          <motion.div
            variants={itemVariants}
            className="flex items-center justify-between mb-4"
          >
            <div className="flex items-center gap-2">
              <motion.img
                whileHover={{ scale: 1.1 }}
                src={getLevelIcon(logDetail?.level)}
                alt="level icon"
                className="w-11 h-11"
              />
              <div className="text-[28px] font-[paperlogy6]">
                {logDetail?.level}
              </div>
            </div>
            <div className="flex items-center gap-2">
              <JiraMakingButton />
              <motion.button
                whileHover={{ scale: 1.1 }}
                whileTap={{ scale: 0.95 }}
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
              </motion.button>
            </div>
          </motion.div>

          {/* 기본 로그 정보 */}
          <motion.div
            variants={sectionVariants}
            className="w-full max-w-[550px] grid grid-cols-1 md:grid-cols-2 gap-4 mb-6 overflow-x-auto"
          >
            <motion.div variants={itemVariants} className="text-left">
              <span className="text-[12px] text-slate-500">타임스탬프</span>
              <div className="font-[paperlogy4]">
                {logDetail?.timestamp
                  ? new Date(logDetail.timestamp)
                      .toLocaleString("ko-KR", {
                        year: "numeric",
                        month: "2-digit",
                        day: "2-digit",
                        hour: "2-digit",
                        minute: "2-digit",
                        second: "2-digit",
                        hour12: false,
                      })
                      .replace(/\. /g, "-")
                      .replace(/\./g, "")
                  : "-"}
              </div>
            </motion.div>
            <motion.div variants={itemVariants} className="text-left">
              <span className="text-[12px] text-slate-500">소스</span>
              <div className="font-[paperlogy4]">
                {logDetail?.source || "-"}
              </div>
            </motion.div>
            <motion.div variants={itemVariants} className="text-left">
              <span className="text-[12px] text-slate-500">환경</span>
              <div className="font-[paperlogy4]">
                {logDetail?.environment || "-"}
              </div>
            </motion.div>
            <motion.div variants={itemVariants} className="text-left">
              <span className="text-[12px] text-slate-500">추적 ID</span>
              <div className="font-[paperlogy4]" title={logDetail?.traceId}>
                {logDetail?.traceId || "-"}
              </div>
            </motion.div>
            <motion.div variants={itemVariants} className="text-left">
              <span className="text-[12px] text-slate-500">로거</span>
              <div className="font-[paperlogy4] break-words">
                {logDetail?.logger && logDetail.logger.length > 20
                  ? logDetail.logger.split(".").map((part, index, array) => (
                      <span key={index}>
                        {part}
                        {index < array.length - 1 && (
                          <>
                            .<br className="md:hidden" />
                          </>
                        )}
                      </span>
                    ))
                  : logDetail?.logger || "-"}
              </div>
            </motion.div>
            <motion.div variants={itemVariants} className="text-left">
              <span className="text-[12px] text-slate-500">로그 타입</span>
              <div className="font-[paperlogy4]">
                {logDetail?.logType || "-"}
              </div>
            </motion.div>
          </motion.div>

          {/* 로그 메세지 섹션 */}
          <motion.div variants={sectionVariants} className="mb-6">
            <div className="text-left p-4 text-[18px] font-[paperlogy6] ">
              MESSAGE
            </div>
            <motion.div
              variants={itemVariants}
              className="max-w-[550px] text-left bg-slate-200/20 p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm break-all"
            >
              {logDetail?.message &&
              logDetail.message.length > 150 &&
              !showFullMessage
                ? logDetail.message.substring(0, 150) + "..."
                : logDetail?.message}

              {logDetail?.message && logDetail.message.length > 150 && (
                <motion.div
                  whileHover={{ scale: 1.02 }}
                  className="flex items-center gap-2 cursor-pointer mt-2 text-lime-600"
                  onClick={() => setShowFullMessage(!showFullMessage)}
                >
                  <span className="font-bold text-[12px]">
                    {showFullMessage ? "접기" : "더보기"}
                  </span>
                  <motion.svg
                    animate={{ rotate: showFullMessage ? 180 : 0 }}
                    transition={{ duration: 0.3 }}
                    xmlns="http://www.w3.org/2000/svg"
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <polyline points="6 9 12 15 18 9"></polyline>
                  </motion.svg>
                </motion.div>
              )}
            </motion.div>
          </motion.div>

          {/* 에러 정보 섹션 - 에러 정보가 있을 때만 표시 */}
          {logDetail?.error && (
            <motion.div variants={sectionVariants} className="mb-6">
              <div className="text-left p-4 text-[18px] font-[paperlogy6]">
                ERROR DETAILS
              </div>
              <motion.div
                variants={itemVariants}
                className="text-left bg-red-200/10 p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm"
              >
                <div className="mb-2">
                  <span className="font-bold">Type: </span>
                  {logDetail.error.type || "-"}
                </div>
                <div className="mb-2">
                  <span className="font-bold">Message: </span>
                  {logDetail.error.message || "-"}
                </div>
                {logDetail.error.stacktrace && (
                  <div>
                    <motion.div
                      whileHover={{ scale: 1.02 }}
                      className="flex items-center gap-2 cursor-pointer"
                      onClick={() => setShowStacktrace(!showStacktrace)}
                    >
                      <span className="font-bold">Stacktrace:</span>
                      <motion.svg
                        animate={{ rotate: showStacktrace ? 180 : 0 }}
                        transition={{ duration: 0.3 }}
                        xmlns="http://www.w3.org/2000/svg"
                        width="16"
                        height="16"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      >
                        <polyline points="6 9 12 15 18 9"></polyline>
                      </motion.svg>
                    </motion.div>
                    {showStacktrace && (
                      <motion.pre
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: "auto" }}
                        exit={{ opacity: 0, height: 0 }}
                        transition={{ duration: 0.3 }}
                        className="mt-2 overflow-x-auto whitespace-pre-wrap text-[12px] bg-[var(--bg)] text-[var(--text)] p-2 rounded"
                      >
                        {logDetail.error.stacktrace}
                      </motion.pre>
                    )}
                  </div>
                )}
              </motion.div>
            </motion.div>
          )}

          {/* 클라이언트 및 HTTP 정보 섹션 */}
          <motion.div variants={sectionVariants} className="mb-6">
            <div className="text-left p-4 text-[18px] font-[paperlogy6]">
              CLIENT & HTTP INFO
            </div>
            <motion.div
              variants={itemVariants}
              className="max-w-[550px] text-left bg-slate-200/20 p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm"
            >
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {logDetail?.client?.url && (
                  <motion.div variants={itemVariants}>
                    <span className="font-bold">URL: </span>
                    <span className="break-all">{logDetail.client.url}</span>
                  </motion.div>
                )}
                {logDetail?.client?.userAgent && (
                  <motion.div variants={itemVariants}>
                    <span className="font-bold">User Agent: </span>
                    <span className="break-all">
                      {logDetail.client.userAgent}
                    </span>
                  </motion.div>
                )}
                {logDetail?.client?.referrer && (
                  <motion.div variants={itemVariants}>
                    <span className="font-bold">Referrer: </span>
                    <span className="break-all">
                      {logDetail.client.referrer}
                    </span>
                  </motion.div>
                )}
                {logDetail?.http?.durationMs !== undefined && (
                  <motion.div variants={itemVariants}>
                    <span className="font-bold">Duration: </span>
                    {logDetail.http.durationMs}ms
                  </motion.div>
                )}
              </div>

              {/* HTTP 요청/응답 정보 */}
              {logDetail?.http && (
                <motion.div
                  variants={itemVariants}
                  className="mt-4 border-t pt-4 border-slate-200"
                >
                  <div className="font-bold mb-2">HTTP 상세 정보:</div>

                  {logDetail.http.request && (
                    <div className="mb-3">
                      <div className="font-semibold">Request:</div>
                      <div className="ml-4 grid grid-cols-1 md:grid-cols-2 gap-2">
                        {logDetail.http.request.method && (
                          <motion.div variants={itemVariants}>
                            <span className="font-medium">Method: </span>
                            {logDetail.http.request.method}
                          </motion.div>
                        )}
                        {logDetail.http.request.url && (
                          <motion.div variants={itemVariants}>
                            <span className="font-medium">URL: </span>
                            <span className="break-all">
                              {logDetail.http.request.url}
                            </span>
                          </motion.div>
                        )}
                      </div>
                    </div>
                  )}

                  {logDetail.http.response && (
                    <div>
                      <div className="font-semibold">Response:</div>
                      <div className="ml-4">
                        {logDetail.http.response.statusCode && (
                          <motion.div variants={itemVariants}>
                            <span className="font-medium">Status Code: </span>
                            {logDetail.http.response.statusCode}
                          </motion.div>
                        )}
                      </div>
                    </div>
                  )}
                </motion.div>
              )}
            </motion.div>
          </motion.div>

          {/* 이벤트 정보 섹션 */}
          {logDetail?.event && (
            <motion.div variants={sectionVariants} className="mb-6">
              <div className="text-left p-4 text-[18px] font-[paperlogy6]">
                EVENT INFO
              </div>
              <motion.div
                variants={itemVariants}
                className="max-w-[550px] text-left bg-slate-100/50 p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm"
              >
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-3">
                  {logDetail.event.type && (
                    <motion.div variants={itemVariants}>
                      <span className="font-bold">Type: </span>
                      {logDetail.event.type}
                    </motion.div>
                  )}
                  {logDetail.event.targetSelector && (
                    <motion.div variants={itemVariants}>
                      <span className="font-bold">Target: </span>
                      <span className="break-all">
                        {logDetail.event.targetSelector}
                      </span>
                    </motion.div>
                  )}
                </div>

                {logDetail.event.properties &&
                  Object.keys(logDetail.event.properties).length > 0 && (
                    <motion.div variants={itemVariants} className="mt-2">
                      <div className="font-bold mb-2">Properties:</div>
                      {renderJsonObject(logDetail.event.properties)}
                    </motion.div>
                  )}
              </motion.div>
            </motion.div>
          )}

          {/* 페이로드 정보 섹션 */}
          {logDetail?.payload && Object.keys(logDetail.payload).length > 0 && (
            <motion.div variants={sectionVariants} className="mb-6">
              <div className="text-left p-4 text-[18px] font-[paperlogy6]">
                PAYLOAD
              </div>
              <motion.div
                variants={itemVariants}
                className="max-w-[550px] text-left bg-slate-100/50 p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm"
              >
                {renderJsonObject(logDetail.payload)}
              </motion.div>
            </motion.div>
          )}

          {/* CHO:LOG EXPLAIN 섹션 */}
          <motion.div variants={sectionVariants} className="mb-8">
            <div className="text-left p-4 text-[18px] font-[paperlogy6]">
              CHO:LOG EXPLAIN
            </div>
            <motion.div
              variants={itemVariants}
              className="cursor-pointer"
              onClick={handleExplanationClick}
            >
              {isExplanationLoading ? (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="flex gap-5 h-full px-6 py-3 text-[14px] shadow-sm hover:bg-lime-200/50 transition-all bg-lime-200/20 rounded-xl"
                >
                  <motion.div
                    animate={{ rotate: 360 }}
                    transition={{
                      repeat: Infinity,
                      duration: 1,
                      ease: "linear",
                    }}
                    className="rounded-full h-6 w-6 border-b-5 border-lime-600"
                  ></motion.div>
                  <span>분석중이다굴~!</span>
                </motion.div>
              ) : showExplanation ? (
                <motion.div
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.3 }}
                  className="text-[14px] text-left font-[consolaNormal] px-6 py-3 shadow-sm hover:bg-lime-200/50 transition-all bg-lime-200/20 rounded-lg"
                >
                  {explanationError ? (
                    <span className="text-red-500">{explanationError}</span>
                  ) : explanation ? (
                    explanation.split("\n").map((line, index) => (
                      <React.Fragment key={index}>
                        {line}
                        {index < explanation.split("\n").length - 1 && <br />}
                      </React.Fragment>
                    ))
                  ) : (
                    "분석실패했다굴... 너무 어려운 로그 아닌가굴..."
                  )}
                </motion.div>
              ) : (
                <motion.div
                  whileHover={{ scale: 1.03 }}
                  className="flex justify-end gap-5"
                >
                  <motion.div className="h-full text-left px-6 py-3 text-[14px] shadow-sm hover:bg-lime-200/50 transition-all bg-lime-200/20 rounded-3xl">
                    <div>도움이 필요하면</div>
                    <div>나를 클릭하라굴~!</div>
                  </motion.div>
                  <motion.div
                    animate={{ y: [0, -5, 0] }}
                    transition={{ repeat: Infinity, duration: 1.5 }}
                    className="w-20"
                  >
                    <img src={frogimg} alt="개구리" />
                  </motion.div>
                </motion.div>
              )}
            </motion.div>
          </motion.div>
        </motion.div>

        {/* 관련 로그 섹션 */}
        <motion.div
          variants={sectionVariants}
          className="flex-1 rounded-lg p-4 md:p-6 shadow-sm min-w-[280px] bg-white/5 border border-[var(--line)]"
        >
          <h2 className="text-left text-base md:text-lg font-[paperlogy6] mb-4 md:mb-6">
            Related Log
          </h2>
          <motion.div
            variants={containerVariants}
            className="space-y-3 md:space-y-4"
          >
            {!isLoading &&
              traceLogs?.map((log, index, array) => (
                <motion.div
                  variants={itemVariants}
                  whileHover={{
                    backgroundColor: "rgba(163, 230, 53, 0.1)",
                  }}
                  onClick={() => handleclick(log.id)}
                  key={index}
                  className="flex items-start gap-2 md:gap-3 text-[var(--text)] cursor-pointer hover:bg-lime-200/20 p-1 rounded transition-colors"
                >
                  <div className="relative flex items-center flex-shrink-0">
                    <div className="w-5 h-5 relative flex items-center justify-center">
                      <img
                        src={getLevelIcon(log.level)}
                        alt={`${log.level} icon`}
                        className="w-4 h-4 md:w-5 md:h-5 object-contain"
                      />
                    </div>
                    {index !== array.length - 1 && (
                      <motion.div
                        initial={{ height: 0 }}
                        animate={{ height: "3rem" }}
                        transition={{ duration: 0.5, delay: index * 0.1 }}
                        className="absolute h-12 md:h-14 w-[2px] bg-[var(--helpertext)]"
                        style={{ left: "50%", zIndex: -1, top: "50%" }}
                      ></motion.div>
                    )}
                  </div>
                  <div className="text-xs md:text-sm flex-shrink-0 min-w-[30px] md:min-w-[50px]">
                    {log.source
                      ? log.source.toLowerCase() === "backend"
                        ? "BE"
                        : log.source.toLowerCase() === "frontend"
                          ? "FE"
                          : log.source
                      : "-"}
                  </div>
                  <div className="text-xs md:text-sm truncate max-w-[120px] sm:max-w-[160px] md:max-w-[200px]">
                    {log.message}
                  </div>
                </motion.div>
              ))}
          </motion.div>
        </motion.div>

        {/* 아카이빙 모달 */}
        <ArchiveModal
          logId={logDetail?.id}
          projectId={projectId} // projectId 추가
          isOpen={isArchiveModalOpen}
          onClose={() => setIsArchiveModalOpen(false)}
          onArchive={handleArchive}
        />
      </div>
    </motion.div>
  );
};

export default LogPage;
