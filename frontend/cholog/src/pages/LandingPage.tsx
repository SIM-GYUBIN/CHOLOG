/**
 * 프로젝트 소개 페이지
 * @returns 프로젝트 랜딩 페이지
 */
import EachLog from "../components/eachLog";

const LandingPage = () => {
  return (
    <div>
      <h1>프로젝트 랜딩 페이지</h1>
      <EachLog
        id="1"
        timestamp="2024-03-19T15:30:00Z"
        message="GET request to /api/users failed"
        apiPath="/api/users"
        level="INFO"
        traceId="trace-abc-123"
        spanId="span-xyz-789"
        details={{
          errorCode: "404",
          stackTrace: "Error: GET request to /api/users failed\n    at ApiClient.get (/src/api/client.ts:25:7)\n    at UserService.getUsers (/src/services/users.ts:12:29)"
        }}
      />
    </div>
  );
};

export default LandingPage;
