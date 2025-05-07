
import EachLog from '../components/eachLog';

const mockLogs = [
  {
    _id: "trace-12345-span-67890",
    from: "FE",
    timestamp: "2025-04-28T12:00:00Z",
    message: "java.lang.NullPointerException at ...",
    level: "ERROR",
  },
  {
    _id: "trace-54321-span-09876",
    from: "BE",
    timestamp: "2025-04-28T11:58:00Z",
    message: "로그인 성공",
    level: "INFO",
  },
  {
    _id: "trace-98765-span-43210",
    from: "BE",
    timestamp: "2025-04-28T11:55:00Z",
    message: "Database connection established",
    level: "DEBUG",
  },
  {
    _id: "trace-24680-span-13579",
    from: "FE",
    timestamp: "2025-04-28T11:52:00Z",
    message: "Warning: Memory usage exceeds 80%",
    level: "WARN",
  },
  {
    _id: "trace-11111-span-22222",
    from: "BE",
    timestamp: "2025-04-28T11:50:00Z",
    message: "System crash detected",
    level: "FATAL",
  },
  {
    _id: "trace-33333-span-44444",
    from: "FE",
    timestamp: "2025-04-28T11:48:00Z",
    message: "API request completed",
    level: "TRACE",
  },
  {
    _id: "trace-55555-span-66666",
    from: "BE",
    timestamp: "2025-04-28T11:45:00Z",
    message: "User authentication successful",
    level: "INFO",
  },
  {
    _id: "trace-77777-span-88888",
    from: "FE",
    timestamp: "2025-04-28T11:42:00Z",
    message: "Component rendering error",
    level: "ERROR",
  },
  {
    _id: "trace-99999-span-00000",
    from: "BE",
    timestamp: "2025-04-28T11:40:00Z",
    message: "Cache cleared successfully",
    level: "DEBUG",
  },
  {
    _id: "trace-12121-span-34343",
    from: "FE",
    timestamp: "2025-04-28T11:38:00Z",
    message: "Network connection timeout",
    level: "WARN",
  }
];


const LogList = () => {
  return (
    <div className='rounded-[24px] p-4'>
        <div className='font-[paperlogy6] text-start mx-3 text-[24px] text-slate-600 mb-2'>Log</div>
    <div className="border border-slate-200 rounded-[24px] shadow p-4 h-[50vh] overflow-y-auto">
              <div className="flex text-start items-center gap-10 border-b-2 border-slate-300 px-3 py-4 w-full text-slate-600 font-[paperlogy7] text-[16px]">
                <div className="basis-[15%]">Level</div>
                <div className="basis-[10%]">Part</div>
                <div className="basis-[50%]">Message</div>
                <div className="basis-[25%]">Date</div>
              </div>
        {mockLogs.map(log => (
            <EachLog
              key={log._id}
              islevelBg={true}
              id={log._id}
              from={log.from}
              timestamp={log.timestamp}
              message={log.message}
              level={log.level as "ERROR" | "INFO" | "WARN" | "DEBUG" | "FATAL" | "TRACE"}
            />
          ))}
    </div>
    </div>
  )
}
export default LogList;