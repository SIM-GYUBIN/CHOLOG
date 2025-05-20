import React, { useEffect, useState, useRef } from "react";
import { useParams } from "react-router-dom";
import DonutChart from "../components/charts/DonutChart";
import ErrorCountChart from "../components/charts/MonthlyLogCountChart";
import ProjectNavBar from "../components/projectNavbar";
import RankingCardList from "../components/common/RankingCardList";
import { useDispatch, useSelector } from "react-redux";
import { AppDispatch, RootState } from "../store/store";
import { fetchReportDetail } from "../store/slices/reportSlice";
import { fetchProjectDetail } from "../store/slices/projectSlice";
import { motion } from "framer-motion";

import html2canvas from "html2canvas";
import jsPDF from "jspdf";

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
  const { currentReport: reportData } = useSelector(
    (state: RootState) => state.report
  );
  const currentProject = useSelector((state: RootState) =>
    state.project.projects.find((p) => p.id === Number(projectId))
  );

  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const reportContentRef = useRef<HTMLDivElement>(null);
  const [isGeneratingPdf, setIsGeneratingPdf] = useState(false);

  useEffect(() => {
    if (projectId) {
      dispatch(fetchProjectDetail(Number(projectId)));
    }
  }, [dispatch, projectId]);

  const handleGenerateReport = () => {
    if (!projectId) return;
    if (!startDate || !endDate) {
      alert("날짜를 입력해주세요.");
      return;
    }

    dispatch(
      fetchReportDetail({
        projectId: parseInt(projectId, 10),
        startDate,
        endDate,
      })
    );
  };

  // PDF 다운로드 함수 추가
  const handleDownloadPdf = async () => {
    if (!reportContentRef.current || isGeneratingPdf || !reportData) {
      if (!reportData) {
        alert("먼저 리포트를 생성해주세요.");
      }
      return;
    }

    setIsGeneratingPdf(true);
    const reportElement = reportContentRef.current;

    try {
      // 차트 등의 요소가 완전히 렌더링될 시간을 잠시 줍니다. (선택 사항)
      // 좀 더 확실한 방법은 각 차트 컴포넌트의 렌더링 완료 시점을 아는 것이지만,
      // 간단하게 setTimeout을 사용하거나, 사용자가 버튼을 누르는 시점에는
      // 대부분 렌더링이 완료되어 있을 것으로 가정합니다.
      await new Promise((resolve) => setTimeout(resolve, 500));

      const canvas = await html2canvas(reportElement, {
        // scale: 2, // 높은 해상도를 위해 scale 조정
        useCORS: true, // 외부 이미지가 있다면 필요 (현재 코드에는 명시적 외부 이미지는 없음)
        // Tailwind CSS의 var(--bg) 같은 CSS 변수 배경색을 html2canvas가 잘 인식하는지,
        // 또는 브라우저의 계산된 스타일을 잘 가져오는지에 따라 배경색이 결정됩니다.
        // 필요시 backgroundColor: '#FFFFFF' 등을 명시할 수 있습니다.
      });

      const imgData = canvas.toDataURL("image/png");
      const pdf = new jsPDF({
        orientation: "p", // 세로 (portrait)
        unit: "mm", // 단위
        format: "a4", // 용지 크기
      });

      const pdfPageWidth = pdf.internal.pageSize.getWidth();
      const pdfPageHeight = pdf.internal.pageSize.getHeight();
      const imgWidth = canvas.width;
      const imgHeight = canvas.height;
      const ratio = imgWidth / imgHeight;
      const scaledImgHeight = pdfPageWidth / ratio; // PDF 너비에 맞춘 이미지 높이

      let position = 0; // 이미지의 현재 y 위치 (잘라낼 부분의 시작점)

      // 이미지가 페이지 높이보다 클 경우 여러 페이지에 걸쳐 추가
      if (scaledImgHeight > pdfPageHeight) {
        while (position < scaledImgHeight) {
          pdf.addImage(
            imgData,
            "PNG",
            0,
            -position,
            pdfPageWidth,
            scaledImgHeight
          );
          position += pdfPageHeight;

          if (position < scaledImgHeight) {
            // 아직 남은 이미지가 있다면 새 페이지 추가
            pdf.addPage();
          }
        }
      } else {
        // 이미지가 한 페이지에 들어갈 경우
        pdf.addImage(imgData, "PNG", 0, 0, pdfPageWidth, scaledImgHeight);
      }

      // 동적 파일명 (프로젝트명과 리포트 기간 사용)
      const projectName =
        currentProject?.name?.replace(/\s+/g, "_") || "Report";
      const periodString = reportData?.periodDescription
        ? reportData.periodDescription
            .replace(/\s*~\s*/, "_to_")
            .replace(/\s+/g, "_")
            .replace(/[^\w-]/g, "")
        : `${startDate}_to_${endDate}`.replace(/[^\w-]/g, "");
      pdf.save(`${projectName}_Report_${periodString}.pdf`);
    } catch (error) {
      console.error("PDF 생성 중 오류 발생:", error);
      alert("PDF를 생성하는 중 오류가 발생했습니다. 다시 시도해주세요.");
    } finally {
      setIsGeneratingPdf(false);
    }
  };

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
    : "기간 정보가 없습니다.";

  return (
    <motion.div
      className="max-w-[65vw] mx-auto"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      <div className="flex flex-col">
        <ProjectNavBar />

        <motion.div
          className="flex flex-row justify-between mb-4"
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

        <motion.div
          className="flex items-center gap-4 mb-6"
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            max={todayString}
            className="px-3 py-2 border border-[var(--line)] rounded-md bg-[var(--bg)] text-sm text-[var(--text)]"
          />
          <span className="text-[var(--text)]">~</span>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            max={todayString}
            className="px-3 py-2 border border-[var(--line)] rounded-md bg-[var(--bg)] text-sm text-[var(--text)]"
          />
          <button
            onClick={handleGenerateReport}
            className="px-4 py-2 bg-lime-500 text-white rounded-lg hover:bg-lime-600 transition-colors"
          >
            리포트 생성
          </button>
        </motion.div>
        {!reportData ? (
          <div className="text-center text-gray-500 mt-10">
            아직 리포트가 생성되지 않았습니다.
          </div>
        ) : (
          <>
            <motion.div
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

            <motion.div
              className="grid grid-cols-2 gap-6"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.45 }}
            >
              <div className="bg-white/5 border border-[var(--line)] rounded-2xl p-6">
                <h2 className="text-xl font-semibold mb-6 text-[var(--text)]">
                  로그 레벨 분포
                </h2>
                <DonutChart data={logData} size={200} thickness={12} />
              </div>

              <div className="bg-white/5 border border-[var(--line)] rounded-2xl p-6">
                <h2 className="text-xl font-semibold mb-6 text-[var(--text)]">
                  로그 발생 추이
                </h2>
                <ErrorCountChart
                  projectId={parseInt(projectId!, 10)}
                  token={localStorage.getItem("token") ?? ""}
                />
              </div>
            </motion.div>

            <motion.div
              className="grid grid-cols-2 gap-6 mt-6"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.5 }}
            >
              <div className="bg-white/5 border border-[var(--line)] rounded-2xl p-6">
                <h2 className="text-xl font-semibold mb-6 text-[var(--text)]">
                  자주 발생하는 에러
                </h2>
                <RankingCardList items={topErrors} />
              </div>
              <div className="bg-white/5 border border-[var(--line)] rounded-2xl p-6">
                <h2 className="text-xl font-semibold mb-6 text-[var(--text)]">
                  응답이 느린 API
                </h2>
                <RankingCardList
                  items={topApis}
                  renderItem={(item) => (
                    <div className="flex flex-col items-start gap-1">
                      <div className="text-base font-bold text-[var(--text)]">
                        #{item.rank}
                      </div>
                      <div className="text-sm text-[var(--text)]">
                        {item.name.split(" ")[0]}
                      </div>
                      <div className="text-sm text-[var(--text)] break-all">
                        {item.name.split(" ")[1]}
                      </div>
                      <div className="mt-2 text-sm text-[var(--helpertext)] whitespace-pre-line">
                        {item.extra}
                      </div>
                    </div>
                  )}
                />
              </div>
            </motion.div>

            <motion.div
              className="mt-8"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.55 }}
            >
              <div className="text-left px-4 text-[var(--text)] text-[18px] font-[paperlogy6]">
                요약
              </div>
              <div className="text-left bg-lime-50/20 p-4 rounded-lg text-[14px] font-[consolaNormal] text-[var(--text)] shadow-sm">
                {summaryText}
              </div>
              <div className="text-right text-xs text-[var(--helpertext)] mt-2 px-4">
                생성일자:{" "}
                {reportData?.generatedAt
                  ? new Date(reportData.generatedAt).toLocaleString()
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
