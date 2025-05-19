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
    console.log("아카이븴 완료:", reason);
    setIsArchiveModalOpen(false);
    // 필요한 후속 처리
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
      <pre className="text-[12px] bg-slate-100 p-2 rounded overflow-x-auto">
        {JSON.stringify(obj, null, 2)}
      </pre>
    );
  };

  return (
    <div className="w-full min-w-[500px] max-w-[65vw] mx-auto px-4 lg:px-0">
      <ProjectNavBar />

      <div className="flex flex-col lg:flex-row gap-6 mx-auto text-[var(--text)]">
        {/* 메인 로그 섹션 */}
        <div className="flex-[2] bg-white/5 rounded-lg p-6 shadow-sm border border-[var(--line)]">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <img
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
          </div>

          {/* 기본 로그 정보 */}
          <div className="w-full max-w-[550px] grid grid-cols-1 md:grid-cols-2 gap-4 mb-6 overflow-x-auto">
            <div className="text-left">
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
            </div>
            <div className="text-left">
              <span className="text-[12px] text-slate-500">소스</span>
              <div className="font-[paperlogy4]">
                {logDetail?.source || "-"}
              </div>
            </div>
            <div className="text-left">
              <span className="text-[12px] text-slate-500">환경</span>
              <div className="font-[paperlogy4]">
                {logDetail?.environment || "-"}
              </div>
            </div>
            <div className="text-left">
              <span className="text-[12px] text-slate-500">추적 ID</span>
              <div className="font-[paperlogy4]" title={logDetail?.traceId}>
                {logDetail?.traceId || "-"}
              </div>
            </div>
            <div className="text-left">
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
            </div>
            <div className="text-left">
              <span className="text-[12px] text-slate-500">로그 타입</span>
              <div className="font-[paperlogy4]">
                {logDetail?.logType || "-"}
              </div>
            </div>
          </div>

          {/* 로그 메세지 섹션 */}
          <div className="mb-6">
            <div className="text-left p-4 text-[18px] font-[paperlogy6] ">
              MESSAGE
            </div>
            <div className="max-w-[550px] text-left bg-slate-100/50 p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm break-all">
              {logDetail?.message &&
              logDetail.message.length > 150 &&
              !showFullMessage
                ? logDetail.message.substring(0, 150) + "..."
                : logDetail?.message}

              {logDetail?.message && logDetail.message.length > 150 && (
                <div
                  className="flex items-center gap-2 cursor-pointer mt-2 text-lime-600"
                  onClick={() => setShowFullMessage(!showFullMessage)}
                >
                  <span className="font-bold text-[12px]">
                    {showFullMessage ? "접기" : "더보기"}
                  </span>
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className={`transition-transform ${showFullMessage ? "rotate-180" : ""}`}
                  >
                    <polyline points="6 9 12 15 18 9"></polyline>
                  </svg>
                </div>
              )}
            </div>
          </div>

          {/* 에러 정보 섹션 - 에러 정보가 있을 때만 표시 */}
          {logDetail?.error && (
            <div className="mb-6">
              <div className="text-left p-4 text-[18px] font-[paperlogy6]">
                ERROR DETAILS
              </div>
              <div className="text-left bg-red-50 p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm">
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
                    <div
                      className="flex items-center gap-2 cursor-pointer"
                      onClick={() => setShowStacktrace(!showStacktrace)}
                    >
                      <span className="font-bold">Stacktrace:</span>
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        width="16"
                        height="16"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        className={`transition-transform ${showStacktrace ? "rotate-180" : ""}`}
                      >
                        <polyline points="6 9 12 15 18 9"></polyline>
                      </svg>
                    </div>
                    {showStacktrace && (
                      <pre className="mt-2 overflow-x-auto whitespace-pre-wrap text-[12px] bg-slate-100 p-2 rounded">
                        {logDetail.error.stacktrace}
                      </pre>
                    )}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* 클라이언트 및 HTTP 정보 섹션 */}
          <div className="mb-6">
            <div className="text-left p-4 text-[18px] font-[paperlogy6]">
              CLIENT & HTTP INFO
            </div>
            <div className="max-w-[550px] text-left bg-slate-100/50 p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {logDetail?.client?.url && (
                  <div>
                    <span className="font-bold">URL: </span>
                    <span className="break-all">{logDetail.client.url}</span>
                  </div>
                )}
                {logDetail?.client?.userAgent && (
                  <div>
                    <span className="font-bold">User Agent: </span>
                    <span className="break-all">
                      {logDetail.client.userAgent}
                    </span>
                  </div>
                )}
                {logDetail?.client?.referrer && (
                  <div>
                    <span className="font-bold">Referrer: </span>
                    <span className="break-all">
                      {logDetail.client.referrer}
                    </span>
                  </div>
                )}
                {logDetail?.http?.durationMs !== undefined && (
                  <div>
                    <span className="font-bold">Duration: </span>
                    {logDetail.http.durationMs}ms
                  </div>
                )}
              </div>

              {/* HTTP 요청/응답 정보 */}
              {logDetail?.http && (
                <div className="mt-4 border-t pt-4 border-slate-200">
                  <div className="font-bold mb-2">HTTP 상세 정보:</div>

                  {logDetail.http.request && (
                    <div className="mb-3">
                      <div className="font-semibold">Request:</div>
                      <div className="ml-4 grid grid-cols-1 md:grid-cols-2 gap-2">
                        {logDetail.http.request.method && (
                          <div>
                            <span className="font-medium">Method: </span>
                            {logDetail.http.request.method}
                          </div>
                        )}
                        {logDetail.http.request.url && (
                          <div>
                            <span className="font-medium">URL: </span>
                            <span className="break-all">
                              {logDetail.http.request.url}
                            </span>
                          </div>
                        )}
                      </div>
                    </div>
                  )}

                  {logDetail.http.response && (
                    <div>
                      <div className="font-semibold">Response:</div>
                      <div className="ml-4">
                        {logDetail.http.response.statusCode && (
                          <div>
                            <span className="font-medium">Status Code: </span>
                            {logDetail.http.response.statusCode}
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* 이벤트 정보 섹션 */}
          {logDetail?.event && (
            <div className="mb-6">
              <div className="text-left p-4 text-[18px] font-[paperlogy6]">
                EVENT INFO
              </div>
              <div className="max-w-[550px] text-left bg-slate-100/50 p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-3">
                  {logDetail.event.type && (
                    <div>
                      <span className="font-bold">Type: </span>
                      {logDetail.event.type}
                    </div>
                  )}
                  {logDetail.event.targetSelector && (
                    <div>
                      <span className="font-bold">Target: </span>
                      {logDetail.event.targetSelector}
                    </div>
                  )}
                </div>

                {logDetail.event.properties &&
                  Object.keys(logDetail.event.properties).length > 0 && (
                    <div>
                      <div className="font-bold mb-2">Properties:</div>
                      {renderJsonObject(logDetail.event.properties)}
                    </div>
                  )}
              </div>
            </div>
          )}

          {/* 페이로드 정보 섹션 */}
          {logDetail?.payload && Object.keys(logDetail.payload).length > 0 && (
            <div className="mb-6">
              <div className="text-left p-4 text-[18px] font-[paperlogy6]">
                PAYLOAD
              </div>
              <div className="max-w-[550px] text-left bg-slate-100/50 p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm">
                {renderJsonObject(logDetail.payload)}
              </div>
            </div>
          )}

          {/* CHO:LOG EXPLANE 섹션 */}
          <div className="mb-8">
            <div className="text-left p-4 text-[18px] font-[paperlogy6]">
              CHO:LOG EXPLANE
            </div>
            <div className="cursor-pointer" onClick={handleExplanationClick}>
              {isExplanationLoading ? (
                <div className="flex gap-5 h-full px-6 py-3 text-[14px] shadow-sm hover:bg-lime-200/50 transition-all bg-[#F7FEE7] rounded-xl">
                  <div className="animate-spin rounded-full h-6 w-6 border-b-5 border-lime-600"></div>
                  <span>분석중이다굴~!</span>
                </div>
              ) : showExplanation ? (
                <div className="text-[14px] text-left font-[consolaNormal] px-6 py-3 shadow-sm hover:bg-lime-200/50 transition-all bg-[#F7FEE7] rounded-lg">
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

        <div className="flex-1 rounded-lg p-4 md:p-6 shadow-sm min-w-[280px] bg-white/5 border border-[var(--line)]">
          <h2 className="text-left text-base md:text-lg font-[paperlogy6] mb-4 md:mb-6">
            Related Log
          </h2>
          <div className="space-y-3 md:space-y-4">
            {!isLoading &&
              traceLogs?.map((log, index, array) => (
                <div
                  onClick={() => handleclick(log.id)}
                  key={index}
                  className="flex items-start gap-2 md:gap-3 text-[var(--text)] cursor-pointer hover:bg-[#F7FEE7] p-1 rounded transition-colors"
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
                      <div
                        className="absolute h-12 md:h-14 w-[2px] bg-[var(--helpertext)]"
                        style={{ left: "50%", zIndex: -1, top: "50%" }}
                      ></div>
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
                </div>
              ))}
          </div>
        </div>
        {/* 아카이븴 모달 */}
        <ArchiveModal
          logId={logDetail?.id}
          projectId={projectId} // projectId 추가
          isOpen={isArchiveModalOpen}
          onClose={() => setIsArchiveModalOpen(false)}
          onArchive={handleArchive}
        />
      </div>
    </div>
  );
};

export default LogPage;
