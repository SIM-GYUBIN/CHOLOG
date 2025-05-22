import React, { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { AppDispatch, RootState } from "../store/store";
import { fetchJiraIssueInfo, createJiraIssue } from "../store/slices/jiraSlice";

interface JiraUser {
  userName: string;
}

// 이슈 타입 목록
const ISSUE_TYPES = ["에픽", "스토리", "작업", "버그"];

// 이슈 타입 매핑 (한글 -> 영어)
const ISSUE_TYPE_MAPPING: Record<string, string> = {
  에픽: "Epic",
  스토리: "Story",
  작업: "Task",
  버그: "Bug",
};

const JiraMakingButton: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const dispatch = useDispatch<AppDispatch>();
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [users, setUsers] = useState<JiraUser[]>([]);
  const [successInfo, setSuccessInfo] = useState<{
    issueKey: string;
    issueUrl: string;
  } | null>(null);

  // 폼 상태 관리
  const [summary, setSummary] = useState<string>("");
  const [description, setDescription] = useState<string>("");
  const [issueType, setIssueType] = useState<string>(ISSUE_TYPES[0]);
  const [reporterName, setReporterName] = useState<string>("");
  const [assigneeName, setAssigneeName] = useState<string>("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // jiraSlice에서 상태 가져오기
  const { issueInfo, isLoading } = useSelector(
    (state: RootState) => state.jira
  );

  // 모달이 열릴 때 유저 조회 API 호출
  useEffect(() => {
    if (isModalOpen && projectId) {
      fetchJiraUsers();
    }
  }, [isModalOpen, projectId]);

  // Jira 유저 조회 함수 - jiraSlice 사용
  const fetchJiraUsers = async () => {
    if (!projectId) return;

    setError(null);

    try {
      // jiraSlice의 fetchJiraIssueInfo 액션 디스패치
      const resultAction = await dispatch(
        fetchJiraIssueInfo(Number(projectId))
      );

      // 에러 처리
      if (fetchJiraIssueInfo.rejected.match(resultAction)) {
        setError("JIRA 유저 정보를 등록 해주세요.");
      } else if (resultAction.payload && "data" in resultAction.payload) {
        // API 응답 구조에 맞게 사용자 데이터 처리
        const userList = resultAction.payload.data?.userNames || [];
        setUsers(userList);

        if (userList.length > 0) {
          // 첫 번째 유저를 기본값으로 설정
          setReporterName(userList[0].userName);
          setAssigneeName(userList[0].userName);
        }
      }
    } catch (err: any) {
      console.error("Jira 유저 조회 중 오류:", err);
      setError("유저 정보를 불러오는데 실패했습니다.");
    }
  };

  // 모달 열기
  const openModal = () => {
    setIsModalOpen(true);
  };

  // 모달 닫기 및 상태 초기화
  const closeModal = () => {
    setIsModalOpen(false);
    setSummary("");
    setDescription("");
    setIssueType(ISSUE_TYPES[0]);
    setReporterName("");
    setAssigneeName("");
    setError(null);
  };

  // 이슈 생성 핸들러
  const handleCreateIssue = async () => {
    // 필수 필드 검증
    if (!summary.trim()) {
      setError("요약을 입력해주세요.");
      return;
    }

    if (!reporterName) {
      setError("보고자를 선택해주세요.");
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const resultAction = await dispatch(
        createJiraIssue({
          projectId: Number(projectId),
          summary,
          description,
          issueType: ISSUE_TYPE_MAPPING[issueType],
          reporterName,
          assigneeName: assigneeName || undefined,
        })
      );

      if (createJiraIssue.fulfilled.match(resultAction)) {
        // 성공 정보 설정
        setSuccessInfo({
          issueKey: resultAction.payload.data.issueKey,
          issueUrl: resultAction.payload.data.issueUrl,
        });
        // 모달은 닫지 않고 성공 메시지 표시
      } else {
        setError("이슈 생성에 실패했습니다.");
      }
    } catch (err: any) {
      console.error("Jira 이슈 생성 중 오류:", err);
      setError("이슈 생성 중 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      {/* Jira 이슈 생성 버튼 */}
      <button
        onClick={openModal}
        className="px-4 py-2 text-[14px] text-white bg-lime-500 hover:bg-lime-600 rounded-lg transition-colors"
      >
        Jira 이슈 생성
      </button>

      {/* Jira 이슈 생성 모달 */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-[500px] max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-[20px] font-[paperlogy6]  text-black">
                Jira 이슈 생성
              </h2>
              <button
                onClick={closeModal}
                className="p-2 rounded-full hover:bg-slate-100 transition-colors"
              >
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
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>

            {successInfo && (
              <div className="bg-lime-50 border-l-4 border-lime-500 text-lime-700 p-4 mb-4 rounded">
                <div className="flex flex-col gap-2">
                  <p className="text-[14px] font-semibold">
                    이슈가 성공적으로 생성되었습니다!
                  </p>
                  <div className="flex justify-center items-center gap-2 text-[12px]">
                    <span className="">이슈 키: {successInfo.issueKey}</span>
                    <a
                      href={successInfo.issueUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-[14px] font-semibold text-lime-700 hover:text-lime-500 underline"
                    >
                      이슈 바로가기
                    </a>
                  </div>
                </div>
              </div>
            )}

            {error && (
              <div className="bg-red-50 border-l-4 border-red-500 text-red-700 p-4 mb-4 rounded">
                <div className="flex">
                  <div className="flex-shrink-0">
                    <svg
                      className="h-5 w-5 text-red-500"
                      xmlns="http://www.w3.org/2000/svg"
                      viewBox="0 0 20 20"
                      fill="currentColor"
                    >
                      <path
                        fillRule="evenodd"
                        d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                        clipRule="evenodd"
                      />
                    </svg>
                  </div>
                  <div className="ml-3">
                    <p className="text-[14px]">{error}</p>
                  </div>
                </div>
              </div>
            )}

            <div className="space-y-4">
              {/* 요약 (Summary) 입력 필드 */}
              <div className="flex flex-col">
                <label className="text-[14px] text-gray-700 text-left mb-1">
                  요약 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={summary}
                  onChange={(e) => setSummary(e.target.value)}
                  className="font-[paperlogy4] text-[12px] caret-lime-500 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-lime-400 placeholder:text-[12px] placeholder:text-gray-300"
                  placeholder="이슈 요약을 입력하세요"
                  required
                />
              </div>

              {/* 설명 (Description) 입력 필드 */}
              <div className="flex flex-col">
                <label className="text-[14px] text-gray-700 text-left mb-1">
                  설명
                </label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  rows={4}
                  className="font-[paperlogy4] text-[12px] caret-lime-500 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-lime-400 placeholder:text-[12px] placeholder:text-gray-300 resize-none"
                  placeholder="이슈에 대한 상세 설명을 입력하세요"
                />
              </div>

              {/* 이슈 타입 (Issue Type) 선택 드롭다운 */}
              <div className="flex flex-col">
                <label className="text-[14px] text-gray-700 text-left mb-1">
                  이슈 타입 <span className="text-red-500">*</span>
                </label>
                <select
                  value={issueType}
                  onChange={(e) => setIssueType(e.target.value)}
                  className="font-[paperlogy4] text-[12px] caret-lime-500 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-lime-400 bg-white"
                >
                  {ISSUE_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
                </select>
              </div>

              {/* 보고자 (Reporter) 선택 드롭다운 */}
              <div className="flex flex-col">
                <label className="text-[14px] text-gray-700 text-left mb-1">
                  보고자 <span className="text-red-500">*</span>
                </label>
                <select
                  value={reporterName}
                  onChange={(e) => setReporterName(e.target.value)}
                  className="font-[paperlogy4] text-[12px] caret-lime-500 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-lime-400 bg-white"
                  disabled={isLoading || users.length === 0}
                >
                  {isLoading ? (
                    <option>로딩 중...</option>
                  ) : users.length === 0 ? (
                    <option>사용자 없음</option>
                  ) : (
                    users.map((user) => (
                      <option key={user.userName} value={user.userName}>
                        {user.userName}
                      </option>
                    ))
                  )}
                </select>
              </div>

              {/* 담당자 (Assignee) 선택 드롭다운 */}
              <div className="flex flex-col">
                <label className="text-[14px] text-gray-700 text-left mb-1">
                  담당자
                </label>
                <select
                  value={assigneeName}
                  onChange={(e) => setAssigneeName(e.target.value)}
                  className="font-[paperlogy4] text-[12px] caret-lime-500 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-lime-400 bg-white"
                  disabled={isLoading || users.length === 0}
                >
                  <option value="">담당자 없음</option>
                  {isLoading ? (
                    <option>로딩 중...</option>
                  ) : (
                    users.map((user) => (
                      <option key={user.userName} value={user.userName}>
                        {user.userName}
                      </option>
                    ))
                  )}
                </select>
              </div>

              {/* 버튼 그룹 */}
              <div className="flex justify-end gap-2 mt-6">
                <button
                  onClick={closeModal}
                  className="px-4 py-2 text-[14px] text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
                >
                  취소
                </button>
                <button
                  onClick={handleCreateIssue}
                  disabled={isSubmitting || !summary || !reporterName}
                  className={`px-4 py-2 text-[14px] text-white rounded-lg transition-colors ${
                    isSubmitting || !summary || !reporterName
                      ? "bg-gray-300 cursor-not-allowed"
                      : "bg-lime-500 hover:bg-lime-600"
                  }`}
                >
                  {isSubmitting ? "생성 중..." : "이슈 생성"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default JiraMakingButton;
