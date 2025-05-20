import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import DonutChart from "../components/charts/DonutChart";
import ErrorCountChart from "../components/charts/MonthlyLogCountChart";
import ProjectNavBar from "../components/projectNavbar";
import RankingCardList from "../components/common/RankingCardList";
import { useDispatch, useSelector } from "react-redux";
import { AppDispatch, RootState } from "../store/store";
import { fetchReportDetail } from "../store/slices/reportSlice";
import { fetchProjectDetail } from "../store/slices/projectSlice";

const levelColors: Record<string, string> = {
  ERROR: "#FB2C36",
  WARN: "#F0B100",
  INFO: "#2B7FFF",
  DEBUG: "#00C950",
  TRACE: "#00BBA7",
  FATAL: "#AD46FF",
};

// 이번 달의 시작일과 종료일 구하기
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

  useEffect(() => {
    if (projectId) {
      dispatch(fetchProjectDetail(Number(projectId)));
    }
  }, [dispatch, projectId]);

  const handleGenerateReport = () => {
    if (!projectId) return;

    const { startDate: defaultStart, endDate: defaultEnd } =
      getCurrentMonthRange();

    dispatch(
      fetchReportDetail({
        projectId: parseInt(projectId, 10),
        startDate: startDate || defaultStart,
        endDate: endDate || defaultEnd,
      })
    );
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
        `평균 응답 시간: ${(api.averageResponseTimeMs / 1000).toFixed(1)}초`,
      ].join("\n"),
      rank: api.rank,
    })) || [];

  const summaryText = reportData?.periodDescription
    ? `이 리포트는 ${reportData.periodDescription} 기간 동안 수집된 로그 분석 결과입니다.`
    : "기간 정보가 없습니다.";

  return (
    <div className="max-w-[65vw] mx-auto">
      <div className="flex flex-col">
        <ProjectNavBar />

        <div className="flex flex-row justify-between mb-4">
          <div className="flex flex-row items-center gap-2 font-[paperlogy5]">
            <div className="text-[24px] text-[var(--helpertext)]">
              {currentProject?.name ?? "프로젝트명 미확인"}
            </div>
          </div>
        </div>

        {/* 날짜 선택 & 리포트 생성 버튼 */}
        <div className="flex items-center gap-4 mb-6">
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
        </div>

        {/* 총 로그 개요 */}
        <div className="grid grid-cols-3 gap-4 mb-6">
          {["overallTotal", "frontendTotal", "backendTotal"].map((key, idx) => (
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
          ))}
        </div>

        <div className="grid grid-cols-2 gap-6">
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
        </div>

        <div className="grid grid-cols-2 gap-6 mt-6">
          <div className="bg-white/5 border border-[var(--line)] rounded-2xl p-6">
            <h2 className="text-xl font-semibold mb-6 text-[var(--text)]">
              자주 발생하는 에러 TOP 3
            </h2>
            <RankingCardList items={topErrors} />
          </div>
          <div className="bg-white/5 border border-[var(--line)] rounded-2xl p-6">
            <h2 className="text-xl font-semibold mb-6 text-[var(--text)]">
              응답이 느린 API TOP 3
            </h2>
            <RankingCardList
              items={topApis}
              renderItem={(item) => (
                <div className="flex flex-col items-start gap-1">
                  <div className="text-base font-bold text-gray-800">
                    #{item.rank}
                  </div>
                  <div className="text-sm text-gray-800">
                    {item.name.split(" ")[0]}
                  </div>
                  <div className="text-sm text-gray-800 break-all">
                    {item.name.split(" ")[1]}
                  </div>
                  <div className="mt-2 text-sm text-gray-500 whitespace-pre-line">
                    {item.extra}
                  </div>
                </div>
              )}
            />
          </div>
        </div>

        {/* 생성일자 및 요약 */}
        <div className="mt-8">
          <div className="text-left px-4 text-[18px] font-[paperlogy6]">
            요약
          </div>
          <div className="text-left bg-[#F7FEE7] p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm">
            {summaryText}
          </div>
          <div className="text-right text-xs text-[var(--helpertext)] mt-2 px-4">
            생성일자:{" "}
            {reportData?.generatedAt
              ? new Date(reportData.generatedAt).toLocaleString()
              : "-"}{" "}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ReportPage;
