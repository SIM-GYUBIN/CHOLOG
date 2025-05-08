import EachLog from '../components/eachLog';

const mockLogs = [
  {
    _id: "trace-12345-span-67890",
    from: "FE",
    type: "System",
    status:404,
    timestamp: "2025-04-28T12:00:00Z",
    message: "java.lang.NullPointerException at ...",
    level: "ERROR",
  },
  {
    _id: "trace-54321-span-09876",
    from: "BE",
    type: "network",
    status:200,
    timestamp: "2025-04-28T11:58:00Z",
    message: "로그인 성공",
    level: "INFO",
  },
  {
    _id: "trace-98765-span-43210",
    from: "BE",
    type: "network",
    status:200,
    timestamp: "2025-04-28T11:55:00Z",
    message: "Database connection established",
    level: "DEBUG",
  },
  {
    _id: "trace-24680-span-13579",
    from: "FE",
    type: "network",
    status:200,
    timestamp: "2025-04-28T11:52:00Z",
    message: "Warning: Memory usage exceeds 80%",
    level: "WARN",
  },
  {
    _id: "trace-11111-span-22222",
    from: "BE",
    type: "network",
    status:200,
    timestamp: "2025-04-28T11:50:00Z",
    message: "System crash detected",
    level: "FATAL",
  },
  {
    _id: "trace-33333-span-44444",
    from: "FE",
    type: "network",
    status:200,
    timestamp: "2025-04-28T11:48:00Z",
    message: "API request completed",
    level: "TRACE",
  },
  {
    _id: "trace-55555-span-66666",
    from: "BE",
    type: "network",
    status:200,
    timestamp: "2025-04-28T11:45:00Z",
    message: "User authentication successful",
    level: "INFO",
  },
  {
    _id: "trace-77777-span-88888",
    from: "FE",
    type: "network",
    status:200,
    timestamp: "2025-04-28T11:42:00Z",
    message: "Component rendering error",
    level: "ERROR",
  },
  {
    _id: "trace-99999-span-00000",
    from: "BE",
    type: "network",
    status:200,
    timestamp: "2025-04-28T11:40:00Z",
    message: "Cache cleared successfully",
    level: "DEBUG",
  },
  {
    _id: "trace-12121-span-34343",
    from: "FE",
    type: "network",
    status:200,
    timestamp: "2025-04-28T11:38:00Z",
    message: "Network connection timeout",
    level: "WARN",
  }
];


const LogList = () => {
  return (
    <div className='rounded-[24px]'>
      <div className='font-[paperlogy6] text-start mx-3 text-[24px] text-slate-600 mb-2'>Log</div>
      <div className="border border-slate-200 rounded-[24px] shadow p-4 h-[50vh] overflow-y-auto">
        {/* <div className="flex text-start items-center gap-10 border-b-2 border-slate-300 px-3 py-4 w-full text-slate-600 font-[paperlogy7] text-[16px]">
          <div className="basis-[15%]">Level</div>
          <div className="basis-[10%]">Part</div>
          <div className="basis-[50%]">Message</div>
          <div className="basis-[25%]">Date</div>
        </div> */}
      <div className=" grid grid-cols-6 gap-10 border-b-2 border-slate-300 px-3 py-4 w-full text-slate-600 font-[paperlogy7] text-[14px]">

{/* 로그 레벨벨 */}
        <div className="col-span-1 flex flex-row justify-center shrink-0 min-w-0 gap-2">
Level
        </div>

{/* 나머지지 */}
        <div className="col-span-5 gap-10">
          
          <div className="grid grid-cols-10 gap-10">
            <div className="flex justify-center col-span-0.5 shrink-0 min-w-0">
Part            </div>
            <div className="flex justify-center col-span-1.5 shrink-0 min-w-0">
Type            </div>
            <div className="flex justify-center  col-span-1 shrink-0 min-w-0">
Status            </div>
            <div className="flex justify-center col-span-5 text-start min-w-0 truncate px-4">
Message            </div>
            <div className="flex justify-center col-span-2 min-w-0 shrink-0 ">
Date            </div>
            
          </div>
        </div>
      </div>



        {mockLogs.map(log => (
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
      </div>
    </div>
  )
}
export default LogList;