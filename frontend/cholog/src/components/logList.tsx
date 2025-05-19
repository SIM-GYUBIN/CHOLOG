import { useState, useEffect } from "react";
import { useDispatch } from "react-redux";
import { useParams } from "react-router-dom";
import EachLog from "./eachLog";
import { LogDetail } from "../types/log.types";
import { fetchLogs, searchLogs } from "../store/slices/logSlice";
import { AppDispatch } from "../store/store";

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
          message: searchTerm,
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
    if (projectId) {
      if (searchTerm || selectedLevel || selectedSource) {
        dispatch(
          searchLogs({
            projectId: Number(projectId),
            page: pageNumber,
            size: pageSize,
            sort: "timestamp,desc",
            message: searchTerm,
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
        dispatch(
          fetchLogs({
            projectId: Number(projectId),
            page: pageNumber,
            size: pageSize,
            sort: "timestamp,desc",
          })
        );
      }
    }
  };

  return (
    <div className="rounded-[24px]">
      <div className="font-[paperlogy6] text-start mx-3 text-[24px] text-[var(--text)] mb-2">
        Log ({pagination?.totalElements || 0})
      </div>

      {/* 검색 및 필터 섹션 */}
      <div className="flex items-center gap-4 p-6 border-b border-[var(--line)] bg-white/5 rounded-t-2xl">
        <div className="relative flex-1">
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyPress={(e) => e.key === "Enter" && handleSearch()}
            onFocus={() => setIsSearchFocused(true)}
            onBlur={() => setIsSearchFocused(false)}
            placeholder="메시지로 검색..."
            className={`w-full px-4 py-2.5 rounded-xl border ${isSearchFocused ? "border-[#5EA500] ring-1 ring-[#5EA500]" : "border-[var(--line)]"} bg-transparent text-[var(--text)] transition-all duration-200 focus:outline-none hover:border-[#5EA500]`}
          />
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

        <select
          value={selectedLevel}
          onChange={(e) => setSelectedLevel(e.target.value)}
          className="px-4 py-2.5 rounded-xl border border-[var(--line)] bg-transparent text-[var(--text)] hover:border-[#5EA500] transition-all duration-200 focus:outline-none focus:border-[#5EA500] focus:ring-1 focus:ring-[#5EA500]"
        >
          <option value="">레벨 선택</option>
          <option value="TRACE">TRACE</option>
          <option value="DEBUG">DEBUG</option>
          <option value="INFO">INFO</option>
          <option value="WARN">WARN</option>
          <option value="ERROR">ERROR</option>
          <option value="FATAL">FATAL</option>
        </select>

        <select
          value={selectedSource}
          onChange={(e) => setSelectedSource(e.target.value)}
          className="px-4 py-2.5 rounded-xl border border-[var(--line)] bg-transparent text-[var(--text)] hover:border-[#5EA500] transition-all duration-200 focus:outline-none focus:border-[#5EA500] focus:ring-1 focus:ring-[#5EA500]"
        >
          <option value="">소스 선택</option>
          <option value="frontend">Frontend</option>
          <option value="backend">Backend</option>
        </select>

        <button
          onClick={handleSearch}
          className="px-6 py-2.5 bg-[#5EA500] text-white rounded-xl hover:bg-[#4A8300] transition-all duration-200 font-medium min-w-[80px] hover:shadow-lg active:transform active:scale-95"
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
              <div className="text-lg sm:text-xl text-[#5EA500] mb-2">
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
                  className={`px-3 py-1 rounded-lg ${currentPageNum === number ? "bg-[#5EA500] text-white" : "text-[var(--text)] hover:bg-[#5EA50015]"}`}
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
