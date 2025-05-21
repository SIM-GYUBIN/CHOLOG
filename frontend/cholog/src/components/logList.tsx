import { useState, useEffect } from "react";
import { useDispatch } from "react-redux";
import { useParams } from "react-router-dom";
import EachLog from "./eachLog";
import { LogDetail } from "../types/log.types";
import { fetchLogs, searchLogs } from "../store/slices/logSlice";
import { AppDispatch } from "../store/store";
import changeIcon from "../assets/change.svg";

interface LogListProps {
  logs: LogDetail[];
  pagination: {
    pageNumber: number;
    totalPages: number;
    totalElements: number;
    pageSize: number;
    first: boolean;
    last: boolean;
  } | null;
}

const LogList = ({ logs, pagination }: LogListProps) => {
  const dispatch = useDispatch<AppDispatch>();
  const { projectId } = useParams();
  const [currentPage, setCurrentPage] = useState(pagination?.pageNumber || 1);
  const [pageSize] = useState(20);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedLevel, setSelectedLevel] = useState<string>("");
  const [selectedSource, setSelectedSource] = useState<string>("");
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const [searchType, setSearchType] = useState<"message" | "apiPath">(
    "message"
  );

  useEffect(() => {
    if (pagination) {
      setCurrentPage(pagination.pageNumber);
    }
  }, [pagination]);

  const handleSearch = () => {
    if (projectId) {
      dispatch(
        searchLogs({
          projectId: Number(projectId),
          page: currentPage,
          size: pageSize,
          sort: "timestamp,desc",
          message: searchType === "message" ? searchTerm : undefined,
          apiPath: searchType === "apiPath" ? searchTerm : undefined,
          level: selectedLevel as
            | "TRACE"
            | "DEBUG"
            | "INFO"
            | "WARN"
            | "ERROR"
            | "FATAL"
            | undefined,
          source: selectedSource as "frontend" | "backend" | undefined,
        })
      );
    }
  };

  const handlePageChange = (pageNumber: number) => {
    setCurrentPage(pageNumber);
    if (!projectId) return;

    const basePayload = {
      projectId: Number(projectId),
      page: pageNumber,
      size: pageSize,
      sort: "timestamp,desc",
    };

    const hasSearchFilters =
      !!searchTerm || !!selectedLevel || !!selectedSource;

    if (hasSearchFilters) {
      dispatch(
        searchLogs({
          ...basePayload,
          message: searchType === "message" ? searchTerm : undefined,
          apiPath: searchType === "apiPath" ? searchTerm : undefined,
          level: selectedLevel as
            | "TRACE"
            | "DEBUG"
            | "INFO"
            | "WARN"
            | "ERROR"
            | "FATAL"
            | undefined,
          source: selectedSource as "frontend" | "backend" | undefined,
        })
      );
    } else {
      dispatch(fetchLogs(basePayload));
    }
  };

  return (
    <div className="rounded-[24px] border border-[var(--line)]">
      <div className="flex items-center gap-4 p-6 border-b border-[var(--line)] bg-white/5 rounded-t-2xl">
        {/* 검색창 */}
        <div className="relative flex-1 h-11">
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyPress={(e) => e.key === "Enter" && handleSearch()}
            onFocus={() => setIsSearchFocused(true)}
            onBlur={() => setIsSearchFocused(false)}
            placeholder={
              searchType === "message"
                ? "메시지로 검색..."
                : "API 경로로 검색..."
            }
            className={`w-full h-full px-4 pr-28 rounded-xl border ${
              isSearchFocused
                ? "border-lime-500 ring-1 ring-lime-500"
                : "border-[var(--line)]"
            } bg-transparent text-[var(--text)] transition-all duration-200 focus:outline-none hover:border-lime-500`}
          />

          {/* 토글 버튼 */}
          <button
            onClick={() =>
              setSearchType((prev) =>
                prev === "message" ? "apiPath" : "message"
              )
            }
            className="absolute right-11 top-1/2 -translate-y-1/2 flex flex-row items-center gap-1 text-xs text-lime-500 border border-lime-500 rounded-md px-2 py-0.5 hover:bg-[#5EA50015] transition-colors"
          >
            <img src={changeIcon} alt="변경" className="w-4 h-4" />
            {searchType === "message" ? "API" : "MSG"}
          </button>

          {/* 검색 아이콘 */}
          <div className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </div>
        </div>

        {/* 레벨 셀렉터 */}
        <select
          value={selectedLevel}
          onChange={(e) => setSelectedLevel(e.target.value)}
          className="h-11 w-24 text-sm px-2 rounded-xl border border-[var(--line)] bg-transparent text-[var(--text)] hover:border-lime-500 focus:outline-none focus:border-lime-500 focus:ring-1 focus:ring-lime-500"
        >
          <option value="" className="bg-[var(--nav)]">
            레벨
          </option>
          <option value="TRACE" className="bg-[var(--nav)]">
            TRACE
          </option>
          <option value="DEBUG" className="bg-[var(--nav)]">
            DEBUG
          </option>
          <option value="INFO" className="bg-[var(--nav)]">
            INFO
          </option>
          <option value="WARN" className="bg-[var(--nav)]">
            WARN
          </option>
          <option value="ERROR" className="bg-[var(--nav)]">
            ERROR
          </option>
          <option value="FATAL" className="bg-[var(--nav)]">
            FATAL
          </option>
        </select>

        {/* 소스 셀렉터 */}
        <select
          value={selectedSource}
          onChange={(e) => setSelectedSource(e.target.value)}
          className="h-11 w-24 text-sm px-2 rounded-xl border border-[var(--line)] bg-transparent text-[var(--text)] hover:border-lime-500 focus:outline-none focus:border-lime-500 focus:ring-1 focus:ring-lime-500"
        >
          <option value="" className="bg-[var(--nav)]">
            소스
          </option>
          <option value="frontend" className="bg-[var(--nav)]">
            Frontend
          </option>
          <option value="backend" className="bg-[var(--nav)]">
            Backend
          </option>
        </select>

        {/* 검색 버튼 */}
        <button
          onClick={handleSearch}
          className="h-11 px-6 bg-lime-500 text-white rounded-xl hover:bg-[#4A8300] transition-all font-medium min-w-[80px] hover:shadow-lg active:transform active:scale-95"
        >
          검색
        </button>
      </div>

      <div className="p-4 overflow-y-auto">
        <div className="border-b-[var(--helpertext)] px-4 py-2 border-b-2 border-[var(--line)] grid grid-cols-8 font-[paperlogy6] text-[18px] text-[var(--text)] w-full">
          {/* 로그 레벨 */}
          <div className="col-span-1 flex flex-row justify-start items-center shrink-0 w-20 gap-2">
            <div className={`bg-white w-4 h-4 rounded-full`}></div>
            <div className="font-semibold truncate">Level</div>
          </div>

          {/* 나머지 */}
          <div className="flex flex-row items-start col-span-7 gap-10">
            <div className="items-center grid grid-cols-10 gap-10 w-full">
              <div className="col-span-2 shrink-0 min-w-0 truncate">Source</div>
              <div className="col-span-2 shrink-0 min-w-0 truncate">ENV</div>
              <div className="col-span-3 text-start min-w-0 truncate">
                Message
              </div>
              <div className="col-span-3 min-w-0 shrink-0 text-start">Date</div>
            </div>
          </div>
        </div>

        {/* 로그 목록 또는 검색 결과 없음 메시지 */}
        <div className="mt-2">
          {logs.length > 0 ? (
            logs.map((log) => (
              <EachLog
                key={log.id}
                islevelBg={true}
                id={log.id}
                timestamp={log.timestamp}
                message={log.message}
                source={log.source}
                level={log.level}
                environment={log.environment}
              />
            ))
          ) : (
            <div className="flex flex-col items-center justify-center h-48 bg-white/5 rounded-2xl mt-4 border border-[var(--line)]">
              <div className="text-lg sm:text-xl text-lime-500 mb-2">
                검색 결과가 없습니다
              </div>
              <div className="text-sm sm:text-base text-gray-500">
                다른 검색어나 필터를 시도해보세요.
              </div>
            </div>
          )}
        </div>

        {/* 페이지네이션 UI */}
        {pagination && pagination.totalElements > 0 && (
          <div className="flex justify-center items-center gap-2 mt-4">
            <button
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={pagination.first}
              className={`px-3 py-1 rounded-lg ${pagination.first ? "text-gray-400 cursor-not-allowed" : "text-[var(--text)] hover:bg-[#5EA50015]"}`}
            >
              이전
            </button>

            {(() => {
              const totalPages = pagination.totalPages;
              const currentPageNum = currentPage;
              const pageNumbers = [];

              // 항상 최대 5개의 페이지 번호를 표시
              let startPage = Math.max(1, currentPageNum - 2);
              let endPage = Math.min(totalPages, startPage + 4);

              // 끝 페이지가 전체 페이지 수를 초과하지 않도록 조정
              if (endPage - startPage < 4) {
                startPage = Math.max(1, endPage - 4);
              }

              for (let i = startPage; i <= endPage; i++) {
                pageNumbers.push(i);
              }

              return pageNumbers.map((number) => (
                <button
                  key={number}
                  onClick={() => handlePageChange(number)}
                  className={`px-3 py-1 rounded-xl ${currentPageNum === number ? "bg-lime-500 text-white" : "text-[var(--text)] hover:bg-[#5EA50015]"}`}
                >
                  {number}
                </button>
              ));
            })()}

            <button
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={pagination.last}
              className={`px-3 py-1 rounded-lg ${pagination.last ? "text-gray-400 cursor-not-allowed" : "text-[var(--text)] hover:bg-[#5EA50015]"}`}
            >
              다음
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default LogList;
