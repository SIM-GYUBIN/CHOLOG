import { useParams } from "react-router-dom";

interface RelatedLog {
  type: "BE" | "FE";
  message: string;
  level: "error" | "warning" | "info" | "success";
}

import errorIcon from '@/assets/levelicon/error.png';
import warnIcon from '@/assets/levelicon/warn.png';
import infoIcon from '@/assets/levelicon/info.png';
import debugIcon from '@/assets/levelicon/debug.png';
import fatalIcon from '@/assets/levelicon/fatal.png';
import traceIcon from '@/assets/levelicon/trace.png';

const getLevelIcon = (level: string) => {
  switch (level.toUpperCase()) {
    case 'ERROR':
      return errorIcon;
    case 'WARNING':
      return warnIcon;
    case 'INFO':
      return infoIcon;
    case 'DEBUG':
      return debugIcon;
    case 'FATAL':
      return fatalIcon;
    case 'TRACE':
      return traceIcon;
    case 'SUCCESS':
      return infoIcon;
    default:
      return infoIcon;
  }
};

const LogPage = () => {
  const { id } = useParams();

  console.log(id);

  // 임시 데이터
  const logData = {
    type: "ERROR",
    timestamp: "2025-04-23 16:32:12",
    message: "API 호출 실패: Error: 500 Internal Server Error",
  };

  const relatedLogs: RelatedLog[] = [
    { type: "BE", message: "12345 --- Info-8080...", level: "error" },
    { type: "FE", message: "API 호출 실패: Error: 5...", level: "error" },
    { type: "FE", message: "설정 파일이 없습니다. 기본...", level: "warning" },
    { type: "BE", message: "로그인 성공: userId=1234...", level: "info" },
    { type: "BE", message: "로그인 성공: userId=1234...", level: "success" },
    { type: "FE", message: "로그인 성공: userId=1234...", level: "success" },
  ];

  return (
    <div className="flex gap-6 max-w-7xl mx-auto">
      {/* 메인 로그 섹션 */}
      <div className="flex-1 bg-white rounded-lg p-6 border border-[#E5E5E5]">
        <div className="flex items-center gap-2 mb-4">
          <img
            src="/src/assets/levelicon/error.png"
            alt="error icon"
            className="w-10 h-10"
          />
          <h1 className="text-[28px] font-paperlogy7">{logData.type}</h1>
        </div>
        <div className="text-left text-gray-500 mb-6">{logData.timestamp}</div>

       {/* 로그 메세지 섹션 */}
        <div className="mb-8">
          <h2 className="text-left p-4 text-18px font-paperlogy6 text-[#475569]">MESSAGE</h2>
          <div className="text-left bg-[#F8FAFC] p-4 rounded-lg font-mono text-14px shadow-sm">
            {logData.message}
          </div>
        </div>

        {/* 초록 LLM 섹션 */}
        <div className="mb-8">
          <div className="flex p-4 items-center gap-2 text-[#475569]">
            <p>CHO:LOG EXPLANE</p>
          </div>
          <div className="text-left bg-[#F7FEE7] p-4 rounded-lg font-mono text-14px shadow-sm">
            {logData.message}
          </div>
        </div>
      </div>

      {/* 관련 로그 섹션 */}
      <div className="w-96 bg-white rounded-lg p-6 shadow-sm">
        <h2 className="text-left text-18px font-paperlogy6 mb-6">
          Related Log
        </h2>
        <div className="space-y-4">
          {relatedLogs.map((log, index) => (
            <div key={index} className="flex items-start gap-3">
              <img 
                src={getLevelIcon(log.level)} 
                alt={`${log.level} icon`} 
                className="w-4 h-4 mt-1"
              />
              <div className="text-gray-400 text-14px">{log.type}</div>
              <div className="text-14px">{log.message}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default LogPage;
