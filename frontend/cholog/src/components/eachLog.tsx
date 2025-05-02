import errorIcon from '@/assets/levelicon/error.png';
import warnIcon from '@/assets/levelicon/warn.png';
import infoIcon from '@/assets/levelicon/info.png';
import debugIcon from '@/assets/levelicon/debug.png';
import fatalIcon from '@/assets/levelicon/fatal.png';
import traceIcon from '@/assets/levelicon/trace.png';

type LogDetails = {
  errorCode?: string;
  stackTrace?: string;
};

type LogProps = {
  id: string;
  timestamp: string;
  message: string;
  apiPath: string;
  level: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG' | 'FATAL' | 'TRACE';
  traceId: string;
  spanId: string;
  details?: LogDetails; // Changed from object to LogDetails for better type safety
};

const getLevelIcon = (level: string) => {
    switch (level) {
      case 'ERROR':
        return errorIcon;
      case 'WARN':
        return warnIcon;
      case 'INFO':
        return infoIcon;
      case 'DEBUG':
        return debugIcon;
      case 'FATAL':
        return fatalIcon;
      case 'TRACE':
        return traceIcon;
      default:
        return infoIcon;
    }
};

export default function EachLog({ 
  timestamp, 
  message, 
  apiPath, 
  level, 
  traceId, 
  spanId, 
  details 
}: LogProps) {
  const formattedTime = new Date(timestamp).toLocaleString();

  return (
    <div className="border rounded-lg p-4 mb-4 hover:shadow-lg transition-shadow">
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2">
          <img src={getLevelIcon(level)} alt={`${level.toLowerCase()} 아이콘`} />
          <div className="font-semibold">{level}</div>
        </div>
        <div className="text-sm text-blue-900">{formattedTime}</div>
      </div>
      
      <div className="mb-2">
        <div className="text-lg font-medium">{message}</div>
        <div className="text-sm text-gray-600">{apiPath}</div>
      </div>
      
      <div className="text-xs text-gray-500">
        <div>Trace ID: {traceId}</div>
        <div>Span ID: {spanId}</div>
      </div>
      
      {(details?.errorCode || details?.stackTrace) && (
        <div className="mt-2 p-2 bg-gray-50 rounded">
          {details.errorCode && (
            <div className="text-sm text-red-600">Error Code: {details.errorCode}</div>
          )}
          {details.stackTrace && (
            <div className="mt-1 text-xs text-gray-700 overflow-x-auto whitespace-pre-wrap">
              {details.stackTrace}
            </div>
          )}
        </div>
      )}
    </div>
  );
}