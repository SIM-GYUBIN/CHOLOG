import { useState } from "react";
import EachLog from "../components/eachLog";

const mockLogs = [
  {
    _id: "trace-12345-span-67890",
    from: "FE",
    type: "System",
    status: 404,
    timestamp: "2025-04-28T12:00:00Z",
    message: "java.lang.NullPointerException at ...",
    level: "ERROR",
  },
  {
    _id: "trace-54321-span-09876",
    from: "BE",
    type: "network",
    status: 200,
    timestamp: "2025-04-28T11:58:00Z",
    message: "로그인 성공",
    level: "INFO",
  },
  {
    _id: "trace-98765-span-43210",
    from: "BE",
    type: "network",
    status: 200,
    timestamp: "2025-04-28T11:55:00Z",
    message: "Database connection established",
    level: "DEBUG",
  },
  {
    _id: "trace-24680-span-13579",
    from: "FE",
    type: "network",
    status: 200,
    timestamp: "2025-04-28T11:52:00Z",
    message: "Warning: Memory usage exceeds 80%",
    level: "WARN",
  },
  {
    _id: "trace-11111-span-22222",
    from: "BE",
    type: "network",
    status: 200,
    timestamp: "2025-04-28T11:50:00Z",
    message: "System crash detected",
    level: "FATAL",
  },
  {
    _id: "trace-33333-span-44444",
    from: "FE",
    type: "network",
    status: 200,
    timestamp: "2025-04-28T11:48:00Z",
    message: "API request completed",
    level: "TRACE",
  },
  {
    _id: "trace-55555-span-66666",
    from: "BE",
    type: "network",
    status: 200,
    timestamp: "2025-04-28T11:45:00Z",
    message: "User authentication successful",
    level: "INFO",
  },
  {
    _id: "trace-77777-span-88888",
    from: "FE",
    type: "network",
    status: 200,
    timestamp: "2025-04-28T11:42:00Z",
    message: "Component rendering error",
    level: "ERROR",
  },
  {
    _id: "trace-99999-span-00000",
    from: "BE",
    type: "network",
    status: 200,
    timestamp: "2025-04-28T11:40:00Z",
    message: "Cache cleared successfully",
    level: "DEBUG",
  },
  {
    _id: "trace-12121-span-34343",
    from: "FE",
    type: "network",
    status: 200,
    timestamp: "2025-04-28T11:38:00Z",
    message: "Network connection timeout",
    level: "WARN",
  },
  {
    _id: "trace-12121-span-34343",
    from: "FE",
    type: "network",
    status: 200,
    timestamp: "2025-04-28T11:38:00Z",
    message: "Network connection timeout",
    level: "WARN",
  },
  {
    _id: "trace-12121-span-34343",
    from: "FE",
    type: "network",
    status: 200,
    timestamp: "2025-04-28T11:38:00Z",
    message: "Network connection timeout",
    level: "WARN",
  },
];

const LogList = () => {
  const [currentPage, setCurrentPage] = useState(1);
  const logsPerPage = 10;

  // 현재 페이지의 로그 계산
  const indexOfLastLog = currentPage * logsPerPage;
  const indexOfFirstLog = indexOfLastLog - logsPerPage;
  const currentLogs = mockLogs.slice(indexOfFirstLog, indexOfLastLog);

  // 전체 페이지 수 계산
  const totalPages = Math.ceil(mockLogs.length / logsPerPage);

  // 페이지 변경 핸들러
  const handlePageChange = (pageNumber: number) => {
    setCurrentPage(pageNumber);
  };

  return (
    <div className="rounded-[24px]">
      <div className="font-[paperlogy6] text-start mx-3 text-[24px] text-[var(--text)] mb-2">
        Log
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
              <div className="col-span-0.5 shrink-0 min-w-0">Part</div>
              <div className="col-span-1.5 shrink-0 min-w-0">Type</div>
              <div className="col-span-1 shrink-0 min-w-0">Status</div>
              <div className="col-span-4 text-start min-w-0 truncate">Message</div>
              <div className="col-span-3 min-w-0 shrink-0 text-start">Date</div>
            </div>
          </div>
        </div>

        {currentLogs.map((log) => (
          <EachLog
            key={log._id}
            islevelBg={true}
            type={log.type}
            status={log.status}
            id={log._id}
            from={log.from}
            timestamp={log.timestamp}
            message={log.message}
            level={log.level as "ERROR" | "INFO" | "WARN" | "DEBUG" | "FATAL" | "TRACE"}
          />
        ))}

        {/* 페이지네이션 UI */}
        <div className="flex justify-center items-center gap-2 mt-4">
          <button
            onClick={() => handlePageChange(currentPage - 1)}
            disabled={currentPage === 1}
            className={`px-3 py-1 rounded ${currentPage === 1
                ? 'text-gray-400 cursor-not-allowed'
                : 'text-[var(--text)] hover:bg-[#5EA50015]'
              }`}
          >
            이전
          </button>

          {Array.from({ length: totalPages }, (_, i) => i + 1).map((number) => (
            <button
              key={number}
              onClick={() => handlePageChange(number)}
              className={`px-3 py-1 rounded ${currentPage === number
                  ? 'bg-[#5EA500] text-white'
                  : 'text-[var(--text)] hover:bg-[#5EA50015]'
                }`}
            >
              {number}
            </button>
          ))}

          <button
            onClick={() => handlePageChange(currentPage + 1)}
            disabled={currentPage === totalPages}
            className={`px-3 py-1 rounded ${currentPage === totalPages
                ? 'text-gray-400 cursor-not-allowed'
                : 'text-[var(--text)] hover:bg-[#5EA50015]'
              }`}
          >
            다음
          </button>
        </div>
      </div>
    </div>
  );
};

export default LogList;
