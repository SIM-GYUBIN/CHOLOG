import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import EachLog from '../components/eachLog';
import ProjectNavBar from '../components/projectNavbar';
import { useDispatch, useSelector } from 'react-redux';
import { fetchArchivedLogs } from '../store/slices/logSlice';
import { RootState, AppDispatch } from '../store/store';

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

interface PaginationInfo {
  pageNumber: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
  first: boolean;
  last: boolean;
}

interface ApiResponse {
  success: boolean;
  data: {
    content: ArchiveLog[];
    pageNumber: number;
    totalPages: number;
    totalElements: number;
    pageSize: number;
    first: boolean;
    last: boolean;
  };
  timestamp: string;
}

export default function ArchiveListPage() {
  const { projectId } = useParams();
  const dispatch = useDispatch<AppDispatch>();
  const { archivedLogs, isLoading, error } = useSelector((state: RootState) => state.log);
  const [expandedReasons, setExpandedReasons] = useState<Record<string, boolean>>({});

  useEffect(() => {
    if (projectId) {
      dispatch(fetchArchivedLogs({ projectId, page: 1, size: 10 }));
    }
  }, [projectId, dispatch]);

  const handlePageChange = (newPage: number) => {
    if (projectId) {
      dispatch(fetchArchivedLogs({ projectId, page: newPage, size: 10 }));
    }
  };

  const toggleReason = (logId: string) => {
    setExpandedReasons((prev) => ({
      ...prev,
      [logId]: !prev[logId],
    }));
  };

  // 페이지네이션 정보 추출
  const pagination = archivedLogs || {
    pageNumber: 1,
    totalPages: 1,
    totalElements: 0,
    pageSize: 10,
    first: true,
    last: true
  };

  // 아카이브 로그 컨텐츠 추출
  const archiveLogs = archivedLogs?.content || [];

  return (
    <div className="w-full lg:w-[70vw] mx-auto">
      <ProjectNavBar />

      <div className="text-[28px] font-[paperlogy6] my-6">Log Archive</div>

      {isLoading && <div className="text-center py-8">로딩 중...</div>}

      {error && (
        <div className="text-red-500 text-center py-4">
          {error.message} (Code: {error.code})
        </div>
      )}

      {!isLoading && !error && (
        <>
          <div className="border rounded-xl border-slate-200">
            {archiveLogs.map((log, index) => (
              <div key={log.logId}>
                <div className={`flex ${index !== 0 ? 'border-t' : ''} border-slate-200`}>
                  <div className="w-full py-2">
                    <EachLog
                      id={log.logId}
                      timestamp={log.logTimestamp}
                      level={log.logLevel}
                      message={log.logMessage}
                      environment={log.logEnvironment}
                      source={log.logSource}
                      islevelBg={false}
                    />
                  </div>
                </div>
                <div className="p-4 text-[14px] text-slate-600">
                  <div className="mx-4 text-start p-2">
                    <div
                      className={`${!expandedReasons[log.logId] ? 'line-clamp-2' : ''
                        } break-keep font-[paperlogy4] tracking-wider`}
                      onClick={() => toggleReason(log.logId)}
                    >
                      {log.memo}
                    </div>
                    <div className="flex justify-end">
                      {log.memo.length > 100 && (
                        <button
                          onClick={() => toggleReason(log.logId)}
                          className="text-[12px] text-green-600 mt-1 hover:underline cursor-pointer"
                        >
                          {expandedReasons[log.logId] ? '접기' : '더보기'}
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {archiveLogs.length > 0 && (
            <div className="flex justify-center text-center gap-2 mt-6 text-[12px]">
              <button
                onClick={() => handlePageChange(pagination.pageNumber - 1)}
                disabled={pagination.first}
                className={`px-3 py-1 rounded-xl ${pagination.first ? 'bg-slate-100 text-slate-400' : 'text-slate-600 hover:bg-slate-200'
                  }`}
              >
                이전
              </button>

              <div className="flex gap-1">
                {Array.from({ length: pagination.totalPages }, (_, i) => i + 1).map((page) => (
                  <button
                    key={page}
                    onClick={() => handlePageChange(page)}
                    className={`px-3 py-1 rounded-xl ${page === pagination.pageNumber
                        ? 'bg-[rgba(101,218,94,1)] text-white'
                        : 'bg-white text-slate-600 hover:bg-slate-200'
                      }`}
                  >
                    {page}
                  </button>
                ))}
              </div>

              <button
                onClick={() => handlePageChange(pagination.pageNumber + 1)}
                disabled={pagination.last}
                className={`px-3 py-1 rounded-xl ${pagination.last ? 'bg-slate-100 text-slate-400' : 'bg-white text-slate-600 hover:bg-slate-200'
                  }`}
              >
                다음
              </button>
            </div>
          )}

          {archiveLogs.length === 0 && (
            <div className="text-center py-8 text-gray-500">
              아카이브된 로그가 없습니다.
            </div>
          )}
        </>
      )}
    </div>
  );
}
