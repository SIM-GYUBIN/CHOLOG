import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import DonutChart from "../components/charts/DonutChart";
import ErrorChart from "../components/charts/ErrorChart";
import ProjectNavBar from "../components/projectNavbar";
import RankingCardList from "../components/common/RankingCardList";
import { useDispatch, useSelector } from "react-redux";
import { AppDispatch, RootState } from "../store/store";
import {
  fetchReportDetail,
  downloadPdfReport,
  clearPdfError,
  DownloadPdfParams,
} from "../store/slices/reportSlice";
import { fetchProjectDetail } from "../store/slices/projectSlice";
import { motion } from "framer-motion";

const levelColors: Record<string, string> = {
  ERROR: "#FB2C36",
  WARN: "#F0B100",
  INFO: "#2B7FFF",
  DEBUG: "#00C950",
  TRACE: "#00BBA7",
  FATAL: "#AD46FF",
};

const getCurrentMonthRange = (): { startDate: string; endDate: string } => {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), 1);
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  return {
    startDate: start.toISOString().split("T")[0],
    endDate: end.toISOString().split("T")[0],
  };
};
const todayString = new Date().toISOString().split("T")[0];

const ReportPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const dispatch = useDispatch<AppDispatch>();

  // Redux 스토어에서 상태 가져오기
  const {
    currentReport: reportData,
    isLoading: isReportLoading, // 리포트 데이터 로딩 상태
    error: reportError, // 리포트 데이터 로딩 오류
    isGeneratingPdf, // PDF 생성 로딩 상태 (Redux 스토어에서 관리)
    pdfError, // PDF 생성 오류 (Redux 스토어에서 관리)
    reportGeneratedStartDate, // 현재 리포트가 생성된 시작일
    reportGeneratedEndDate, // 현재 리포트가 생성된 종료일
  } = useSelector((state: RootState) => state.report);

  const currentProject = useSelector((state: RootState) =>
    state.project.projects.find((p) => p.id === Number(projectId))
  );

  // UI에서 날짜 선택을 위한 로컬 상태
  const [localStartDate, setLocalStartDate] = useState("");
  const [localEndDate, setLocalEndDate] = useState("");

  // const reportContentRef = useRef<HTMLDivElement>(null); // 더 이상 직접 사용하지 않음
  // const [isGeneratingPdf, setIsGeneratingPdf] = useState(false); // Redux 스토어로 대체

  useEffect(() => {
    if (projectId) {
      // 컴포넌트 마운트 시 또는 projectId 변경 시 프로젝트 상세 정보 가져오기
      dispatch(fetchProjectDetail(Number(projectId))); // 이 부분은 필요에 따라 유지 또는 삭제
      // 만약 초기 리포트 로딩 로직이 필요하다면 여기에 추가
      // 예: dispatch(fetchReportDetail({ projectId: Number(projectId), ...getCurrentMonthRange() }));
    }
  }, [dispatch, projectId]);

  // PDF 생성 오류 처리
  useEffect(() => {
    if (pdfError) {
      alert(`PDF 생성 중 오류가 발생했습니다: ${pdfError}`);
      dispatch(clearPdfError()); // 오류 메시지 표시 후 Redux 상태 초기화
    }
  }, [pdfError, dispatch]);

  // 리포트 데이터 생성 (JSON 조회) 함수
  const handleGenerateReport = () => {
    if (!projectId || isReportLoading) return;
    const { startDate: defaultStart, endDate: defaultEnd } =
      getCurrentMonthRange();
    dispatch(
      fetchReportDetail({
        projectId: parseInt(projectId, 10),
        startDate: localStartDate || defaultStart,
        endDate: localEndDate || defaultEnd,
      })
    );
  };

  // PDF 다운로드 함수 (Redux Thunk 디스패치)
  const handleDownloadPdf = () => {
    if (isGeneratingPdf || !reportData || !projectId || isReportLoading) {
      if (!reportData) {
        alert("PDF로 만들 리포트 내용이 없습니다. 먼저 리포트를 생성해주세요.");
      }
      return;
    }

    const htmlContent = document.documentElement.outerHTML;

    // Thunk에 전달할 파라미터 구성
    // PDF 생성 시점의 날짜는 현재 조회된 리포트의 날짜를 사용하는 것이 일관성 있음
    const startDateForPdf =
      reportGeneratedStartDate ||
      localStartDate ||
      getCurrentMonthRange().startDate;
    const endDateForPdf =
      reportGeneratedEndDate || localEndDate || getCurrentMonthRange().endDate;

    const params: DownloadPdfParams = {
      projectId: parseInt(projectId, 10),
      htmlContent,
      startDate: startDateForPdf,
      endDate: endDateForPdf,
    };
    dispatch(downloadPdfReport(params));
  };

  // 데이터 가공 로직 (logData, topErrors, topApis, summaryText)은 이전과 동일
  const logData =
    reportData?.logLevelDistribution.distribution.map((item) => ({
      name: item.level,
      value: item.count,
      color: levelColors[item.level] || "#999999",
    })) || [];

  const topErrors =
    reportData?.topErrors.map((e) => ({
      name: e.errorIdentifier,
      count: e.occurrenceCount,
    })) || [];

  const topApis =
    reportData?.slowBackendApis.map((api) => ({
      name: `${api.httpMethod} ${api.requestPath}`,
      count: 0,
      extra: [
        `요청 수: ${api.totalRequests}회`,
        `평균 응답 시간: ${(api.averageResponseTimeMs / 1000).toFixed(3)}초`,
        `최대 응답 시간: ${(api.maxResponseTimeMs / 1000).toFixed(3)}초`,
      ].join("\n"),
      rank: api.rank,
    })) || [];

  const summaryText = reportData?.periodDescription
    ? `이 리포트는 ${reportData.periodDescription} 기간 동안 수집된 로그 분석 결과입니다.`
    : "기간 정보가 없습니다. 리포트를 생성해주세요.";

  return (
    <motion.div
      className="max-w-[65vw] mx-auto"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      {/* ProjectNavBar는 PDF에 포함되지 않는다고 가정 */}
      <ProjectNavBar />

      {/* 제목 부분 */}
      <motion.div
        className="flex flex-row justify-between my-4 items-center" // my-4와 items-center 추가
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.2 }}
      >
        <div className="flex flex-row items-center gap-2 font-[paperlogy5]">
          <div className="text-[24px] text-[var(--helpertext)]">
            {currentProject?.name ?? "프로젝트명 미확인"} 리포트
          </div>
        </div>
      </motion.div>

      {/* 날짜 선택 & 버튼 영역 */}
      <motion.div
        className="flex items-center gap-4 mb-6"
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
      >
        <input
          type="date"
          value={localStartDate} // 로컬 상태 바인딩
          onChange={(e) => setLocalStartDate(e.target.value)}
          max={todayString}
          className="px-3 py-2 border border-[var(--line)] rounded-md bg-[var(--bg)] text-sm text-[var(--text)]"
        />
        <span className="text-[var(--text)]">~</span>
        <input
          type="date"
          value={localEndDate} // 로컬 상태 바인딩
          onChange={(e) => setLocalEndDate(e.target.value)}
          max={todayString}
          className="px-3 py-2 border border-[var(--line)] rounded-md bg-[var(--bg)] text-sm text-[var(--text)]"
        />
        <button
          onClick={handleGenerateReport}
          disabled={isReportLoading} // 리포트 데이터 로딩 중 비활성화
          className="px-4 py-2 bg-lime-500 text-white rounded-lg hover:bg-lime-600 transition-colors text-sm"
        >
          {isReportLoading ? "리포트 생성 중..." : "리포트 생성"}
        </button>
        <button
          onClick={handleDownloadPdf} // 수정된 PDF 다운로드 함수 호출
          disabled={isGeneratingPdf || !reportData || isReportLoading} // PDF 생성 중, 데이터 없거나, 리포트 로딩 중일 때 비활성화
          className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors text-sm disabled:bg-gray-400"
        >
          {isGeneratingPdf ? "PDF 생성 중..." : "PDF 다운로드"}
        </button>
      </motion.div>

      {/* reportError 메시지 표시 (선택적 UI) */}
      {reportError && (
        <div className="mb-4 p-3 bg-red-100 text-red-700 border border-red-300 rounded-md">
          리포트 조회 중 오류: {reportError}
        </div>
      )}

      {/* 리포트 내용 (실제 PDF로 변환될 주요 영역) */}
      {/* document.documentElement.outerHTML을 사용하므로 특정 ref는 필요 없음 */}
      <div id="report-main-flex-container" className="flex flex-col">
        {/* 리포트 데이터가 없을 때 안내 메시지 */}
        {!isReportLoading && !reportData && !reportError && (
          <motion.div
            className="text-center py-10 text-[var(--text)]"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.4 }}
          >
            날짜를 선택하고 "리포트 생성" 버튼을 눌러 리포트를 조회해주세요.
          </motion.div>
        )}

        {/* 리포트 데이터가 있을 때만 내용 표시 */}
        {reportData && (
          <>
            <motion.div /* 총 로그 개요 */
              className="grid grid-cols-3 gap-4 mb-6"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
            >
              {["overallTotal", "frontendTotal", "backendTotal"].map(
                (key, idx) => (
                  <div
                    key={key}
                    className="bg-white/5 border border-[var(--line)] rounded-2xl p-4"
                  >
                    <p className="text-sm text-[var(--helpertext)] mb-1">
                      {["전체 로그 수", "프론트엔드 로그", "백엔드 로그"][idx]}
                    </p>
                    <p className="text-xl font-semibold text-[var(--text)]">
                      {(reportData?.totalLogCounts as any)?.[
                        key
                      ]?.toLocaleString?.() ?? "-"}
                    </p>
                  </div>
                )
              )}
            </motion.div>

            <motion.div /* 로그 레벨 분포 & 로그 발생 추이 */
              className="grid grid-cols-2 gap-6"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.45 }}
            >
              <div className="bg-white/5 border border-[var(--line)] rounded-2xl p-6">
                <h2 className="text-xl font-semibold mb-6 text-[var(--text)]">
                  로그 레벨 분포
                </h2>
                {logData.length > 0 ? (
                  <DonutChart data={logData} size={200} thickness={12} />
                ) : (
                  <p className="text-sm text-[var(--helpertext)]">
                    데이터가 없습니다.
                  </p>
                )}
              </div>

              {projectId && (
                <ErrorChart
                  projectId={parseInt(projectId, 10)}
                  startDate={localStartDate || getCurrentMonthRange().startDate}
                  endDate={localEndDate || getCurrentMonthRange().endDate}
                />
              )}
            </motion.div>

            <motion.div /* 에러 TOP3 & 느린 API TOP3 */
              className="grid grid-cols-2 gap-6 mt-6"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.5 }}
            >
              <div className="bg-white/5 border border-[var(--line)] rounded-2xl p-6">
                <h2 className="text-xl font-semibold mb-6 text-[var(--text)]">
                  자주 발생하는 에러
                </h2>
                {topErrors.length > 0 ? (
                  <RankingCardList items={topErrors} />
                ) : (
                  <p className="text-sm text-[var(--helpertext)]">
                    데이터가 없습니다.
                  </p>
                )}
              </div>
              <div className="bg-white/5 border border-[var(--line)] rounded-2xl p-6">
                <h2 className="text-xl font-semibold mb-6 text-[var(--text)]">
                  응답이 느린 API TOP 5
                </h2>
                {topApis.length > 0 ? (
                  <RankingCardList
                    items={topApis}
                    renderItem={(item) => (
                      <div className="flex flex-col items-start gap-1 text-[var(--text)]">
                        <div className="text-base font-bold">#{item.rank}</div>
                        <div className="text-sm">{item.name.split(" ")[0]}</div>
                        <div className="text-sm break-all">
                          {item.name.split(" ")[1]}
                        </div>
                        <div className="mt-2 text-xs text-[var(--helpertext)] whitespace-pre-line">
                          {item.extra}
                        </div>
                      </div>
                    )}
                  />
                ) : (
                  <p className="text-sm text-[var(--helpertext)]">
                    데이터가 없습니다.
                  </p>
                )}
              </div>
            </motion.div>

            <motion.div /* 요약 및 생성일자 */
              className="mt-8"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.55 }}
            >
              <div className="text-left px-4 text-[18px] font-[paperlogy6] text-[var(--text)]">
                요약
              </div>
              <div className="text-left bg-lime-50 dark:bg-lime-900/30 p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm text-lime-700 dark:text-lime-300">
                {summaryText}
              </div>
              <div className="text-right text-xs text-[var(--helpertext)] mt-2 px-4">
                생성일자:{" "}
                {reportData?.generatedAt
                  ? new Date(reportData.generatedAt).toLocaleString("ko-KR")
                  : "-"}
              </div>
            </motion.div>
          </>
        )}
      </div>
    </motion.div>
  );
};

export default ReportPage;
