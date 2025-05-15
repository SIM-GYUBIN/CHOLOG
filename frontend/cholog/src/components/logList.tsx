import { useState, useEffect } from "react";
import { useDispatch } from "react-redux";
import { useParams } from "react-router-dom";
import EachLog from "./eachLog";
import { LogDetail } from "../types/log.types";
import { fetchLogs } from "../store/slices/logSlice";
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
  const dispatch = useDispatch<AppDispatch>();  // Type the dispatch
  const { projectId } = useParams();
  const [currentPage, setCurrentPage] = useState(pagination?.pageNumber || 1);
  const [pageSize] = useState(20); // API 기본값

  useEffect(() => {
    if (pagination) {
      setCurrentPage(pagination.pageNumber);
    }
  }, [pagination]);

  const handlePageChange = (pageNumber: number) => {
    if (projectId) {
      dispatch(fetchLogs({ 
        projectId: Number(projectId),
        page: pageNumber,
        size: pageSize,
        sort: "timestamp,desc" // 기본 정렬 설정
      }));
      setCurrentPage(pageNumber);
    }
  };

  return (
    <div className="rounded-[24px]">
      <div className="font-[paperlogy6] text-start mx-3 text-[24px] text-[var(--text)] mb-2">
        Log ({pagination?.totalElements || 0})
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
              <div className="col-span-2 shrink-0 min-w-0 truncate">DEV</div>
              <div className="col-span-4 text-start min-w-0 truncate">Message</div>
              <div className="col-span-2 min-w-0 shrink-0 text-start">Date</div>
            </div>
          </div>
        </div>

        {logs.map((log) => (
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
        ))}

        {/* 페이지네이션 UI */}
        {pagination && (
          <div className="flex justify-center items-center gap-2 mt-4">
            <button
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={pagination.first}
              className={`px-3 py-1 rounded ${
                pagination.first
                  ? "text-gray-400 cursor-not-allowed"
                  : "text-[var(--text)] hover:bg-[#5EA50015]"
              }`}
            >
              이전
            </button>

            {Array.from(
              { length: Math.min(5, pagination.totalPages) },
              (_, i) => {
                const start = Math.max(
                  1,
                  currentPage - 2,
                  pagination.totalPages - 4
                );
                return start + i;
              }
            )
              .filter((num) => num <= pagination.totalPages)
              .map((number) => (
                <button
                  key={number}
                  onClick={() => handlePageChange(number)}
                  className={`px-3 py-1 rounded ${
                    currentPage === number
                      ? "bg-[#5EA500] text-white"
                      : "text-[var(--text)] hover:bg-[#5EA50015]"
                  }`}
                >
                  {number}
                </button>
              ))}

            <button
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={pagination.last}
              className={`px-3 py-1 rounded ${
                pagination.last
                  ? "text-gray-400 cursor-not-allowed"
                  : "text-[var(--text)] hover:bg-[#5EA50015]"
              }`}
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
