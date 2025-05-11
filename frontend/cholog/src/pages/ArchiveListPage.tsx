import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import EachLog from '../components/eachLog';
import ProjectNavBar from '../components/projectNavbar';

interface ArchiveLog {
  logId: string;
  timestamp: string;
  apiPath: string;
  level: "TRACE" | "DEBUG" | "INFO" | "WARN" | "ERROR" | "FATAL";
  from: "BE" | "FE";
  type: string;
  status: number;
  message: string;
  archiveReason: string;
  archivedAt: string;
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
  const [archiveLogs, setArchiveLogs] = useState<ArchiveLog[]>([]);
  const [expandedReasons, setExpandedReasons] = useState<{[key: string]: boolean}>({});
  const [pagination, setPagination] = useState<PaginationInfo>({
    pageNumber: 1,
    totalPages: 1,
    totalElements: 0,
    pageSize: 10,
    first: true,
    last: true,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 목업 데이터 추가
  const mockArchiveData: ApiResponse = {
    success: true,
    data: {
      content: [
        {
          logId: "trace-12345-span-67890",
          timestamp: "2025-04-28T12:00:00Z",
          apiPath: "/api/user/login",
          level: "ERROR",
          from: "BE",
          type: "HTTP",
          status: 500,
          message: "Internal Server Error during login process",
          archiveReason: "반복적인 로그인 실패 이슈 추적을 위한 아카이브",
          archivedAt: "2025-04-29T09:00:00Z"
        },
        {
          logId: "trace-23456-span-78901",
          timestamp: "2025-04-27T15:30:00Z",
          apiPath: "/api/order/checkout",
          level: "WARN",
          from: "BE",
          type: "HTTP",
          status: 400,
          message: "Invalid order data format",
          archiveReason: "결제 프로세스 개선을 위한 로그 보관",
          archivedAt: "2025-04-28T10:00:00Z"
        },
        {
          logId: "trace-34567-span-89012",
          timestamp: "2025-04-27T14:20:00Z",
          apiPath: "/api/product/search",
          level: "INFO",
          from: "FE",
          type: "HTTP",
          status: 200,
          message: "Search request completed successfully",
          archiveReason: "검색 성능 분석을 위한 로그 저장",
          archivedAt: "2025-04-28T11:15:00Z"
        },
        {
          logId: "trace-45678-span-90123",
          timestamp: "2025-04-27T13:45:00Z",
          apiPath: "/api/cart/update",
          level: "DEBUG",
          from: "BE",
          type: "HTTP",
          status: 200,
          message: "Cart update operation completed",
          archiveReason: "장바구니 업데이트 로직 분석",
          archivedAt: "2025-04-28T12:30:00Z"
        },
        {
          logId: "trace-56789-span-01234",
          timestamp: "2025-04-27T12:10:00Z",
          apiPath: "/api/notification/send",
          level: "FATAL",
          from: "BE",
          type: "HTTP",
          status: 503,
          message: "Notification service unavailable",
          archiveReason: "긴급 알림 발송 실패 분석",
          archivedAt: "2025-04-28T13:45:00Z"
        }
      ],
      pageNumber: 1,
      totalPages: 3,
      totalElements: 15,
      pageSize: 5,
      first: true,
      last: false
    },
    timestamp: "2025-04-29T10:00:00Z"
  };

  const fetchArchiveLogs = async (page: number = 1) => {
    try {
      setLoading(true);
      setError(null);
      
      // 목업 데이터에서 페이지별 데이터 생성
      const pageSize = 5;
      const totalItems = 15;
      const totalPages = Math.ceil(totalItems / pageSize);
      
      // 페이지별 다른 데이터 생성
      const mockPageData = {
        ...mockArchiveData,
        data: {
          ...mockArchiveData.data,
          content: mockArchiveData.data.content.map((item, index) => {
            const archiveReasons = [
              "반복적인 로그인 실패 이슈 추적을 위한 아카이브, 개 긴 문장을 가정해보자반복적인 로그인 실패 이슈 추적을 위한 아카이브, 개 긴 문장을 가정해보자반복적인 로그인 실패 이슈 추적을 위한 아카이브, 개 긴 문장을 가정해보자반복적인 로그인 실패 이슈 추적을 위한 아카이브, 개 긴 문장을 가정해보자반복적인 로그인 실패 이슈 추적을 위한 아카이브, 개 긴 문장을 가정해보자반복적인 로그인 실패 이슈 추적을 위한 아카이브, 개 긴 문장을 가정해보자반복적인 로그인 실패 이슈 추적을 위한 아카이브, 개 긴 문장을 가정해보자",
              "결제 프로세스 개선을 위한 로그 보관",
              "검색 성능 분석을 위한 로그 저장",
              "장바구니 업데이트 로직 분석",
              "긴급 알림 발송 실패 분석"
            ];
            
            return {
              ...item,
              logId: `trace-${page}-${index}`,
              timestamp: new Date(Date.now() - (index * 1000 * 60 * 60 * 24)).toISOString(),
              archiveReason: archiveReasons[index % archiveReasons.length]
            };
          }),
          pageNumber: page,
          totalPages: totalPages,
          first: page === 1,
          last: page === totalPages
        }
      };

      setTimeout(() => {
        setArchiveLogs(mockPageData.data.content);
        setPagination({
          pageNumber: mockPageData.data.pageNumber,
          totalPages: mockPageData.data.totalPages,
          totalElements: mockPageData.data.totalElements,
          pageSize: mockPageData.data.pageSize,
          first: mockPageData.data.first,
          last: mockPageData.data.last,
        });
        setLoading(false);
      }, 100);

    } catch (err) {
      setError(err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.');
      setLoading(false);
    }
  };

  useEffect(() => {
    if (projectId) {
      fetchArchiveLogs();
    }
  }, [projectId]);

  const handlePageChange = (newPage: number) => {
    fetchArchiveLogs(newPage);
  };

  const toggleReason = (logId: string) => {
    setExpandedReasons(prev => ({
      ...prev,
      [logId]: !prev[logId]
    }));
  };

  return (
    <div className="w-full lg:w-[70vw] mx-auto">
          <ProjectNavBar />

      <div className="text-[28px] font-[paperlogy6] my-6">Log Archive</div>
      
      {loading && (
        <div className="text-center py-8">로딩 중...</div>
      )}

      {error && (
        <div className="text-red-500 text-center py-4">{error}</div>
      )}

      {!loading && !error && (
        <>
          <div className="border rounded-xl border-slate-200">
            {archiveLogs.map((log, index) => (
              <div key={log.logId} className="">
                <div className={`flex ${index !== 0 ? 'border-t' : ''} border-slate-200`}>
                  <div className="w-full py-2">
                    <EachLog
                      id={log.logId}
                      timestamp={log.timestamp}
                      level={log.level}
                      message={log.message}
                      apiPath={log.apiPath}
                      from={log.from}
                      type={log.type}
                      status={log.status}
                      islevelBg={false}
                    />
                  </div>
                </div>
                <div className="p-4  text-[14px] text-slate-600">
                    <div className=''>
                      {/* <div className="col-span-1 text-slate-500">내용 : </div> */}
                      <div className='mx-4  text-start p-2'>
                        <div 
                          className={`${!expandedReasons[log.logId] ? 'line-clamp-2' : ''} break-keep font-[paperlogy4] tracking-wider`}
                          onClick={() => toggleReason(log.logId)}
                        >
                          {log.archiveReason}
                        </div>
                        <div className='flex justify-end'>     
                        {log.archiveReason.length > 100 && (
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
                    {/* <div className='flex flex-row justify-end text-right'>
                    일시 : {new Date(log.archivedAt).toLocaleString('ko-KR', {
                          year: 'numeric',
                          month: '2-digit',
                          day: '2-digit',
                          hour: '2-digit',
                          minute: '2-digit',
                          hour12: false
                        })}
                    </div> */}
                </div>
              </div>
            ))}
          </div>

          {/* 페이지네이션 */}
          <div className="flex justify-center text-center gap-2 mt-6  text-[12px]">
            <button
              onClick={() => handlePageChange(pagination.pageNumber - 1)}
              disabled={pagination.first}
              className={`px-3 py-1 rounded-xl ${
                pagination.first
                  ? 'bg-slate-100 text-slate-400'
                  : 'text-slate-600 hover:bg-slate-200'
              }`}
            >
              이전
            </button>
            
            <div className="flex gap-1">
              {Array.from({ length: pagination.totalPages }, (_, i) => i + 1).map((page) => (
                <button
                  key={page}
                  onClick={() => handlePageChange(page)}
                  className={`px-3 py-1 rounded-xl  ${
                    page === pagination.pageNumber
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
              className={`px-3 py-1 rounded-xl ${
                pagination.last
                  ? 'bg-slate-100 text-slate-400'
                  : 'bg-white text-slate-600 hover:bg-slate-200'
              }`}
            >
              다음
            </button>
          </div>
        </>
      )}
    </div>
  );
}