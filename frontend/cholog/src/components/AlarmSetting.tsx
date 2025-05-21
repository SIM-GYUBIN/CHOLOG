import React, { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { AppDispatch, RootState } from "../store/store";
import { saveWebhook, updateWebhook } from "../store/slices/webhookSlice";
import {
  fetchJiraUserSettings,
  createJiraUserSettings,
  updateJiraUserSettings,
  fetchJiraProjectSettings,
  createJiraProjectSettings,
  updateJiraProjectSettings,
} from "../store/slices/jiraSlice";
import URLGuideModal from "./URLGuideModal";
import JiraGuideModal from "./JiraGuideModal";
import { useParams } from "react-router-dom";
import domainhelpimg from "@/assets/jiraimg/domain_projectkey.png";
import useremailhelpimg from "@/assets/jiraimg/user email.png";
import { motion, AnimatePresence } from "framer-motion"; // framer-motion 추가

interface AlarmSettingProps {
  isOpen: boolean;
  onClose: () => void;
  webhookData?: {
    exists: boolean;
    webhookItem?: {
      id: number;
      mmURL: string;
      keywords: string;
      notificationENV: string;
      isEnabled: boolean;
    };
  };
}

const AlarmSetting: React.FC<AlarmSettingProps> = ({
  isOpen,
  onClose,
  webhookData,
}) => {
  const dispatch = useDispatch<AppDispatch>();
  const { projectId } = useParams<{ projectId: string }>();



  
  // exists 값에 따라 초기값 설정
  const [mmURL, setMmURL] = useState("");
  const [keywords, setKeywords] = useState("");
  const [notificationENV, setNotificationENV] = useState("prod"); // 기본값 설정
  const [isEnabled, setIsEnabled] = useState(true);
  const [isURLGuideOpen, setIsURLGuideOpen] = useState(false);
  const [isJiraGuideOpen, setIsJiraGuideOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<"webhook" | "jira">("webhook");
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Jira 관련 상태
  const [jiraUsername, setJiraUsername] = useState("");
  const [jiraToken, setJiraToken] = useState("");
  const [jiraInstanceUrl, setJiraInstanceUrl] = useState(""); // 추가: Jira 도메인 URL
  const [jiraProjectKey, setJiraProjectKey] = useState("");
  const [jiraProjectEnabled, setJiraProjectEnabled] = useState(false);
  const [jiraActiveSection, setJiraActiveSection] = useState<
    "personal" | "project"
  >("personal");
  const [jiraUserExists, setJiraUserExists] = useState(false);
  const [jiraProjectExists, setJiraProjectExists] = useState(false);
  const [jiraLoading, setJiraLoading] = useState(false);
  const [jiraError, setJiraError] = useState("");
  
  // 성공 메시지 상태 추가
  const [jiraSuccessInfo, setJiraSuccessInfo] = useState<string | null>(null);

  const [isDomainHelpModalOpen, setIsDomainHelpModalOpen] =
    useState<boolean>(false);
  const [isEmailHelpModalOpen, setIsEmailHelpModalOpen] =
    useState<boolean>(false);

  const openDomainHelpModal = () => {
    setIsDomainHelpModalOpen(true);
  };

  const closeDomainHelpModal = () => {
    setIsDomainHelpModalOpen(false);
  };
  const openEmailHelpModal = () => {
    setIsEmailHelpModalOpen(true);
  };

  const closeEmailHelpModal = () => {
    setIsEmailHelpModalOpen(false);
  };

  // 폼 유효성 검사 상태
  const [isFormValid, setIsFormValid] = useState(false);
  const [errors, setErrors] = useState({
    mmURL: "",
    keywords: "",
    jiraUsername: "",
    jiraToken: "",
    jiraInstanceUrl: "", // 추가: Jira 도메인 URL 에러
    jiraProjectKey: "",
  });

  // Jira 상태 가져오기
  const jira = useSelector((state: any) => state.jira);



// 성공 메시지 설정 시 타이머 설정
useEffect(() => {
  if (jiraSuccessInfo) {
    const timer = setTimeout(() => {
      setJiraSuccessInfo(null);
    }, 1500); // 1.5초 후 메시지 사라짐
    
    return () => clearTimeout(timer);
  }
}, [jiraSuccessInfo]);



  // Jira 프로젝트 설정 로드
  useEffect(() => {
    if (projectId && isOpen && activeTab === "jira") {
      dispatch(fetchJiraProjectSettings(Number(projectId)))
        .unwrap()
        .then((response) => {
          // API 응답 로그 출력
          console.log("Jira 프로젝트 설정 응답:", response);

          if (response.success && response.data.exists) {
            // 설정이 존재하면 폼 필드에 값 채우기
            setJiraInstanceUrl(response.data.instanceUrl || "");
            setJiraProjectKey(response.data.projectKey || "");
            setJiraProjectExists(true);
          } else {
            // 설정이 존재하지 않으면 초기화
            setJiraInstanceUrl("");
            setJiraProjectKey("");
            setJiraProjectExists(false);
          }
        })
        .catch((error) => {
          console.error("Jira 프로젝트 설정 로드 중 오류:", error);
          setJiraError("설정을 불러오는 중 오류가 발생했습니다.");
        });
    }
  }, [projectId, isOpen, activeTab, dispatch]);

  // webhookData가 변경될 때 폼 데이터 업데이트 및 콘솔 로그 출력
  useEffect(() => {
    // API 응답 결과를 콘솔에 출력
    // console.log('웹훅 설정 API 응답 결과:', webhookData);

    if (webhookData?.exists && webhookData.webhookItem) {
      setMmURL(webhookData.webhookItem.mmURL || "");
      setKeywords(webhookData.webhookItem.keywords || "");
      setNotificationENV(webhookData.webhookItem.notificationENV || "prod");
      setIsEnabled(webhookData.webhookItem.isEnabled ?? true);
    } else {
      // exists가 false인 경우 초기화
      setMmURL("");
      setKeywords("");
      setNotificationENV("prod");
      setIsEnabled(true);
    }
  }, [webhookData]);

  // 모달이 열릴 때 기본 탭(webhook)의 데이터만 로드
  useEffect(() => {
    if (isOpen) {
      setActiveTab("webhook");
      // webhook 데이터는 이미 props로 받아오고 있으므로 추가 로직 필요 없음
    }
  }, [isOpen]);

  // 폼 유효성 검사
  useEffect(() => {
    const validateForm = () => {
      const newErrors = {
        mmURL: "",
        keywords: "",
        jiraUsername: "",
        jiraToken: "",
        jiraInstanceUrl: "", // 추가
        jiraProjectKey: "",
      };

      // URL 유효성 검사
      if (!mmURL) {
        newErrors.mmURL = "Mattermost URL을 입력해주세요";
      } else if (!mmURL.startsWith("https://")) {
        newErrors.mmURL = "URL은 https://로 시작해야 합니다";
      }

      // 키워드 유효성 검사
      if (!keywords) {
        newErrors.keywords = "알림 받을 키워드를 입력해주세요";
      }

      // Jira 사용자 이름 유효성 검사
      if (activeTab === "jira" && jiraActiveSection === "personal") {
        if (!jiraUsername) {
          newErrors.jiraUsername = "Jira 사용자 이름을 입력해주세요";
        } else if (!jiraUsername.includes("@")) {
          newErrors.jiraUsername = "유효한 이메일 형식이 아닙니다";
        }

        // Jira 토큰 유효성 검사
        if (!jiraToken) {
          newErrors.jiraToken = "Jira API 토큰을 입력해주세요";
        }
      }

      // Jira 프로젝트 키 유효성 검사
      if (activeTab === "jira" && jiraActiveSection === "project") {
        if (!jiraInstanceUrl) {
          newErrors.jiraInstanceUrl = "Jira 도메인 URL을 입력해주세요";
        } else if (!jiraInstanceUrl.startsWith("https://")) {
          newErrors.jiraInstanceUrl = "URL은 https://로 시작해야 합니다";
        }

        if (!jiraProjectKey) {
          newErrors.jiraProjectKey = "Jira 프로젝트 키를 입력해주세요";
        }
      }

      setErrors(newErrors);

      // 활성화된 탭과 섹션에 따라 유효성 검사 결과 설정
      if (activeTab === "webhook") {
        setIsFormValid(!newErrors.mmURL && !newErrors.keywords);
      } else if (activeTab === "jira") {
        if (jiraActiveSection === "personal") {
          setIsFormValid(!newErrors.jiraUsername && !newErrors.jiraToken);
        } else {
          setIsFormValid(
            !newErrors.jiraInstanceUrl && !newErrors.jiraProjectKey
          );
        }
      }
    };

    validateForm();
  }, [
    mmURL,
    keywords,
    activeTab,
    jiraActiveSection,
    jiraUsername,
    jiraToken,
    jiraInstanceUrl,
    jiraProjectKey,
  ]);

  // isOpen이 false에서 true로 변경될 때 webhook 탭으로 초기화
  useEffect(() => {
    if (isOpen) {
      setActiveTab("webhook");
    }
  }, [isOpen]);

  // 웹훅 폼 제출 핸들러
  const handleSubmit = async () => {
    if (!isFormValid || !projectId) return;

    setIsSubmitting(true);

    try {
      const webhookItem = {
        mmURL,
        keywords,
        notificationENV,
        isEnabled,
      };

      // exists 값에 따라 PUT 또는 POST 요청 보내기
      if (webhookData?.exists) {
        // 웹훅 데이터가 존재하면 PUT 요청
        await dispatch(
          updateWebhook({
            projectId: Number(projectId),
            webhookItem,
          })
        ).unwrap();
        console.log("웹훅 설정이 성공적으로 수정되었습니다.");
      } else {
        // 웹훅 데이터가 존재하지 않으면 POST 요청
        await dispatch(
          saveWebhook({
            projectId: Number(projectId),
            webhookItem,
          })
        ).unwrap();
        console.log("웹훅 설정이 성공적으로 생성되었습니다.");
      }

      // 성공 시 모달 닫기
      onClose();
    } catch (error) {
      console.error("웹훅 설정 저장 중 오류가 발생했습니다:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Jira 개인 설정 로드
  useEffect(() => {
    if (isOpen && activeTab === "jira" && jiraActiveSection === "personal") {
      dispatch(fetchJiraUserSettings())
        .unwrap()
        .then((response) => {
          // API 응답 로그 출력
          console.log("Jira 개인 설정 응답:", response);

          if (response.success && response.data.exists) {
            // 설정이 존재하면 폼 필드에 값 채우기
            setJiraUsername(response.data.userName || "");
            setJiraToken(response.data.jiraToken || "");
            setJiraUserExists(true);
          } else {
            // 설정이 존재하지 않으면 초기화
            setJiraUsername("");
            setJiraToken("");
            setJiraUserExists(false);
          }
        })
        .catch((error) => {
          console.error("Jira 개인 설정 로드 중 오류:", error);
          setJiraError("설정을 불러오는 중 오류가 발생했습니다.");
        });
    }
  }, [isOpen, activeTab, jiraActiveSection, dispatch]);

  // Jira 개인 설정 저장 핸들러
  const handleJiraUserSubmit = async () => {
    if (!isFormValid) return;

    setIsSubmitting(true);
    setJiraError("");
    setJiraSuccessInfo(null); // 성공 메시지 초기화

    try {
      // 사용자 설정 데이터 준비
      const jiraUserData = {
        userName: jiraUsername,
        jiraToken: jiraToken,
      };

      // 먼저 사용자 설정이 존재하는지 확인
      const userSettingsResponse = await dispatch(
        fetchJiraUserSettings()
      ).unwrap();
      console.log("Jira 개인 설정 조회 응답:", userSettingsResponse);

      if (userSettingsResponse.success && userSettingsResponse.data.exists) {
        // 설정이 존재하면 업데이트
        const updateResponse = await dispatch(
          updateJiraUserSettings(jiraUserData)
        ).unwrap();
        console.log("Jira 개인 설정 수정 응답:", updateResponse);
        console.log("Jira 개인 설정이 성공적으로 수정되었습니다.");
        setJiraSuccessInfo("Jira 개인 설정이 성공적으로 수정되었습니다!"); // 성공 메시지 설정
      } else {
        // 설정이 존재하지 않으면 새로 생성
        const createResponse = await dispatch(
          createJiraUserSettings(jiraUserData)
        ).unwrap();
        console.log("Jira 개인 설정 등록 응답:", createResponse);
        console.log("Jira 개인 설정이 성공적으로 등록되었습니다.");
        setJiraUserExists(true);
        setJiraSuccessInfo("Jira 개인 설정이 성공적으로 등록되었습니다!"); // 성공 메시지 설정
      }
    } catch (error: any) {
      console.error("Jira 개인 설정 저장 중 오류가 발생했습니다:", error);
      setJiraError(error.message || "알 수 없는 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  // Jira 프로젝트 설정 저장 핸들러
  const handleJiraProjectSubmit = async () => {
    if (!isFormValid || !projectId) return;

    setIsSubmitting(true);
    setJiraError("");
    setJiraSuccessInfo(null); // 성공 메시지 초기화

    try {
      const jiraProjectData = {
        instanceUrl: jiraInstanceUrl,
        projectKey: jiraProjectKey,
      };

      // 기존 설정 존재 여부에 따라 PUT 또는 POST 요청
      if (jiraProjectExists) {
        await dispatch(
          updateJiraProjectSettings({
            projectId: Number(projectId),
            settings: jiraProjectData,
          })
        ).unwrap();
        console.log("Jira 프로젝트 설정이 성공적으로 수정되었습니다.");
        setJiraSuccessInfo("Jira 프로젝트 설정이 성공적으로 수정되었습니다!"); // 성공 메시지 설정
      } else {
        await dispatch(
          createJiraProjectSettings({
            projectId: Number(projectId),
            settings: jiraProjectData,
          })
        ).unwrap();
        console.log("Jira 프로젝트 설정이 성공적으로 등록되었습니다.");
        setJiraProjectExists(true);
        setJiraSuccessInfo("Jira 프로젝트 설정이 성공적으로 등록되었습니다!"); // 성공 메시지 설정
      }
    } catch (error: any) {
      console.error("Jira 프로젝트 설정 저장 중 오류가 발생했습니다:", error);
      setJiraError(error.message || "알 수 없는 오류가 발생했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-[var(--bg)] rounded-lg flex w-[700px] text-start text-[var(--text)]">
        {/* 사이드바 */}
        <div className="w-[200px] bg-[var(--sub)] p-4 rounded-l-lg border-r border-[var(--line)]">
          <div className="text-[18px] text-[var(--text)] font-[paperlogy6] mb-6">
            설정
          </div>
          <div className="space-y-2">
            <button
              onClick={() => setActiveTab("webhook")}
              className={`w-full text-start p-2 rounded-lg text-[14px] transition-colors ${
                activeTab === "webhook"
                  ? "bg-lime-500/20 text-[var(--text)] font-[paperlogy6]"
                  : "hover:bg-slate-200/20"
              }`}
            >
              Webhook 설정
            </button>
            <button
              onClick={() => setActiveTab("jira")}
              className={`w-full text-start p-2 rounded-lg text-[14px] transition-colors ${
                activeTab === "jira"
                  ? "bg-lime-500/20 text-[var(--text)] font-[paperlogy6]"
                  : "hover:bg-slate-200/20"
              }`}
            >
              Jira 연동
            </button>
          </div>
        </div>

        {/* 메인 컨텐츠 */}
        <div className="flex-1 p-6">
          {activeTab === "webhook" ? (
            <>
              <div className="text-[18px] font-[paperlogy6] mb-4">Webhook</div>
              <div className="space-y-4">
                <div>
                  <div className="text-[14px] mb-2 flex items-center gap-1">
                    Mattermost URL <span className="text-red-500">*</span>
                    <button
                      onClick={() => setIsURLGuideOpen(true)}
                      className="text-slate-400 hover:text-slate-600"
                    >
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        fill="none"
                        viewBox="0 0 24 24"
                        strokeWidth={2}
                        stroke="darkgray"
                        className="w-4 h-4"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          d="M9.879 7.519c1.171-1.025 3.071-1.025 4.242 0 1.172 1.025 1.172 2.687 0 3.712-.203.179-.43.326-.67.442-.745.361-1.45.999-1.45 1.827v.75M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9 5.25h.008v.008H12v-.008z"
                        />
                      </svg>
                    </button>
                  </div>
                  <input
                    type="text"
                    value={mmURL}
                    onChange={(e) => setMmURL(e.target.value)}
                    placeholder="https://meeting.ssafy.com/hooks/$$URL_ADDRESS"
                    className={`w-full px-3 py-2 border rounded-lg ${errors.mmURL ? "border-red-500" : "border-slate-700"} text-[12px]`}
                    required
                  />
                  {errors.mmURL && (
                    <p className="text-red-500 text-[11px] mt-1">
                      {errors.mmURL}
                    </p>
                  )}
                </div>

                <div>
                  <div className="text-[14px] mb-1">
                    로그 키워드<span className="text-red-500">*</span>
                  </div>
                  <div className="mb-2 text-[12px] text-slate-500 font-[paperlogy4]">
                    웹훅 알림을 받고싶은 로그 키워드를 입력해주세요
                  </div>
                  {/* 키워드 api요청할때 ","포함해서 그냥 텍스트 자체로 보내기!! */}
                  <input
                    type="text"
                    value={keywords}
                    onChange={(e) => setKeywords(e.target.value)}
                    placeholder="Timeout, Unauthorized, Not_found ..."
                    className={`w-full px-3 py-2 border rounded-lg ${errors.keywords ? "border-red-500" : "border-slate-700"} text-[12px]`}
                    required
                  />
                  {errors.keywords && (
                    <p className="text-red-500 text-[11px] mt-1">
                      {errors.keywords}
                    </p>
                  )}
                  <p className="text-[11px] text-slate-500 mt-1">
                    쉼표(,)로 구분하여 여러 키워드를 입력할 수 있습니다
                  </p>
                </div>

                <div>
                  <div className="text-[14px] mb-2">알림 받을 개발 환경</div>
                  <input
                    type="text"
                    value={notificationENV}
                    onChange={(e) => setNotificationENV(e.target.value)}
                    placeholder="ex) local, prod"
                    className="w-full px-3 py-2 border rounded-lg border-slate-700 text-[12px]"
                  />
                </div>

                <div>
                  <label className="flex items-center gap-5">
                    <div className="text-sm">
                      알림 활성화 <span className="text-red-500">*</span>
                    </div>
                    <label className="relative inline-block w-9 h-6 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={isEnabled}
                        onChange={(e) => setIsEnabled(e.target.checked)}
                        className="sr-only peer"
                        required
                      />
                      <div className="absolute inset-0 bg-[var(--line)] dark:bg-[var(--line)] rounded-full transition-colors peer-checked:bg-lime-600"></div>
                      <div className="absolute left-0.5 top-0.5 w-5 h-5 bg-white dark:bg-gray-50 rounded-full flex items-center justify-center text-[11px] text-lime-700 transition-transform duration-300 peer-checked:translate-x-3">
                        {isEnabled ? "♪" : ""}
                      </div>
                    </label>
                  </label>
                </div>
                <div className="flex justify-end gap-2 mt-6">
                  <button
                    onClick={onClose}
                    className="text-[12px] px-4 text-gray-600 hover:text-[var(--text)]"
                    disabled={isSubmitting}
                  >
                    취소
                  </button>
                  {activeTab === "webhook" && (
                    <button
                      onClick={handleSubmit}
                      disabled={!isFormValid || isSubmitting}
                      className={`text-[12px] px-3 py-2 ${isFormValid && !isSubmitting ? "bg-lime-600 hover:bg-lime-700" : "bg-gray-400 cursor-not-allowed"} text-white rounded-lg transition-colors`}
                    >
                      {isSubmitting
                        ? "처리 중..."
                        : webhookData?.exists
                          ? "수정하기"
                          : "등록하기"}
                    </button>
                  )}
                </div>
              </div>
            </>
          ) : (
            <>
              <div className="text-[18px] font-[paperlogy6] mb-4">Jira 연동</div>
              
              {/* 성공 메시지 표시 부분 */}
              <AnimatePresence>
                {jiraSuccessInfo && (
                  <motion.div
                    className="bg-lime-50 border-l-4 border-lime-500 text-lime-700 p-4 mb-4 rounded"
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    transition={{ duration: 0.3 }}
                  >
                    <div className="flex flex-col gap-2">
                      <p className="text-[14px] font-semibold">{jiraSuccessInfo}</p>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
              
              {/* 에러 메시지 표시 */}
              {jiraError && (
                <div className="bg-red-50 border-l-4 border-red-500 text-red-700 p-4 mb-4 rounded">
                  <p className="text-[14px]">{jiraError}</p>
                </div>
              )}
              
              {/* 탭 메뉴 */}
              <div className="flex border-b border-[var(--line)] mb-6">
                <button
                  onClick={() => setJiraActiveSection("personal")}
                  className={`py-2 px-4 text-[14px] ${
                    jiraActiveSection === "personal"
                      ? "border-b-2 border-lime-500 text-lime-600 font-[paperlogy6]"
                      : "text-[var(--helpertext)] hover:text-slate-400/20"
                  }`}
                >
                  개인 설정
                </button>
                <button
                  onClick={() => setJiraActiveSection("project")}
                  className={`py-2 px-4 text-[14px] ${
                    jiraActiveSection === "project"
                      ? "border-b-2 border-lime-500 text-lime-600 font-[paperlogy6]"
                      : "text-[var(--helpertext)] hover:text-slate-400/20"
                  }`}
                >
                  프로젝트 설정
                </button>
              </div>

              {/* 에러 메시지 표시 */}
              {jiraError && (
                <div className="bg-red-500/10 border border-red-500/40 text-red-600 px-4 py-2 rounded-md mb-4 text-[12px]">
                  {jiraError}
                </div>
              )}

              {/* 로딩 표시 */}
              {jiraLoading && (
                <div className="flex justify-center items-center py-4">
                  <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-lime-500"></div>
                </div>
              )}

              {/* 개인 설정 섹션 */}
              {jiraActiveSection === "personal" && !jiraLoading && (
                <div className="space-y-4">
                  <div>
                    <div className="text-[14px] mb-2 flex items-center gap-1">
                      Jira 사용자 이름 (이메일){" "}
                      <span className="text-red-500">*</span>
                      <button
                        onClick={openEmailHelpModal}
                        className="text-slate-400 hover:text-slate-600"
                      >
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          fill="none"
                          viewBox="0 0 24 24"
                          strokeWidth={2}
                          stroke="darkgray"
                          className="w-4 h-4"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            d="M9.879 7.519c1.171-1.025 3.071-1.025 4.242 0 1.172 1.025 1.172 2.687 0 3.712-.203.179-.43.326-.67.442-.745.361-1.45.999-1.45 1.827v.75M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9 5.25h.008v.008H12v-.008z"
                          />
                        </svg>
                      </button>
                    </div>
                    <input
                      type="email"
                      value={jiraUsername}
                      onChange={(e) => setJiraUsername(e.target.value)}
                      placeholder="example@company.com"
                      className={`w-full px-3 py-2 border rounded-lg ${errors.jiraUsername ? "border-red-500" : "border-slate-700"} text-[12px]`}
                      required
                    />
                    {errors.jiraUsername && (
                      <p className="text-red-500 text-[11px] mt-1">
                        {errors.jiraUsername}
                      </p>
                    )}
                  </div>

                  <div>
                    <div className="text-[14px] mb-2">
                      Jira API 토큰 <span className="text-red-500">*</span>
                    </div>
                    <input
                      type="password"
                      value={jiraToken}
                      onChange={(e) => setJiraToken(e.target.value)}
                      placeholder="Jira API 토큰을 입력하세요"
                      className={`w-full px-3 py-2 border rounded-lg ${errors.jiraToken ? "border-red-500" : "border-slate-700"} text-[12px]`}
                      required
                    />
                    {errors.jiraToken && (
                      <p className="text-red-500 text-[11px] mt-1">
                        {errors.jiraToken}
                      </p>
                    )}
                    <p className="text-[11px] text-slate-500 mt-1">
                      <a
                        href="https://id.atlassian.com/manage-profile/security/api-tokens"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-lime-600 hover:underline"
                      >
                        Jira API 토큰 생성하기
                      </a>
                    </p>
                  </div>
                  <div className="flex justify-end gap-2 mt-6">
                    <button
                      onClick={onClose}
                      className="text-[12px] px-4 text-gray-600 hover:text-[var(--text)]"
                      disabled={isSubmitting}
                    >
                      취소
                    </button>
                    <button
                      onClick={handleJiraUserSubmit}
                      disabled={!isFormValid || isSubmitting}
                      className={`text-[12px] px-3 py-2 rounded-lg text-white ${
                        !isFormValid || isSubmitting
                          ? "bg-slate-300 cursor-not-allowed"
                          : "bg-lime-500 hover:bg-lime-600"
                      }`}
                    >
                      {isSubmitting ? "저장 중..." : "저장하기"}
                    </button>
                  </div>
                </div>
              )}

              {/* 프로젝트 설정 섹션 */}
              {jiraActiveSection === "project" && !jiraLoading && (
                <div className="space-y-4">
                  <div>
                    <div className="text-[14px] mb-2 flex items-center gap-1">
                      Jira 도메인 URL <span className="text-red-500">*</span>
                      <button
                        onClick={openDomainHelpModal}
                        className="text-slate-400 hover:text-slate-600"
                      >
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          fill="none"
                          viewBox="0 0 24 24"
                          strokeWidth={2}
                          stroke="darkgray"
                          className="w-4 h-4"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            d="M9.879 7.519c1.171-1.025 3.071-1.025 4.242 0 1.172 1.025 1.172 2.687 0 3.712-.203.179-.43.326-.67.442-.745.361-1.45.999-1.45 1.827v.75M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9 5.25h.008v.008H12v-.008z"
                          />
                        </svg>
                      </button>
                    </div>
                    <input
                      type="text"
                      value={jiraInstanceUrl}
                      onChange={(e) => setJiraInstanceUrl(e.target.value)}
                      placeholder="https://your-domain.atlassian.net"
                      className={`w-full px-3 py-2 border rounded-lg ${errors.jiraInstanceUrl ? "border-red-500" : "border-slate-700"} text-[12px]`}
                      required
                    />
                    {errors.jiraInstanceUrl && (
                      <p className="text-red-500 text-[11px] mt-1">
                        {errors.jiraInstanceUrl}
                      </p>
                    )}
                  </div>

                  <div>
                    <div className="text-[14px] mb-2">
                      Jira 프로젝트 키 <span className="text-red-500">*</span>
                    </div>
                    <input
                      type="text"
                      value={jiraProjectKey}
                      onChange={(e) => setJiraProjectKey(e.target.value)}
                      placeholder="프로젝트 키를 입력하세요 (예: PROJ)"
                      className={`w-full px-3 py-2 border rounded-lg ${errors.jiraProjectKey ? "border-red-500" : "border-slate-700"} text-[12px]`}
                      required
                    />
                    {errors.jiraProjectKey && (
                      <p className="text-red-500 text-[11px] mt-1">
                        {errors.jiraProjectKey}
                      </p>
                    )}
                  </div>

                  <div className="flex justify-end gap-2 mt-6">
                    <button
                      onClick={onClose}
                      className="text-[12px] px-4 text-gray-600 hover:text-gray-800"
                      disabled={isSubmitting}
                    >
                      취소
                    </button>
                    <button
                      onClick={handleJiraProjectSubmit}
                      disabled={!isFormValid || isSubmitting}
                      className={`text-[12px] px-3 py-2 rounded-lg text-white ${
                        !isFormValid || isSubmitting
                          ? "bg-slate-300 cursor-not-allowed"
                          : "bg-lime-500 hover:bg-lime-600"
                      }`}
                    >
                      {isSubmitting ? "저장 중..." : "저장하기"}
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>
      <URLGuideModal
        isOpen={isURLGuideOpen}
        onClose={() => setIsURLGuideOpen(false)}
      />
      <JiraGuideModal
        isOpen={isJiraGuideOpen}
        onClose={() => setIsJiraGuideOpen(false)}
      />
      {/* 도메인 도움말 이미지 모달 */}
      {isDomainHelpModalOpen && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-[60]">
          <div className="bg-white rounded-lg p-6 w-auto max-w-[90vw] max-h-[90vh] overflow-auto">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-[16px] font-[paperlogy6]">
                Jira 도메인 URL 안내
              </h3>
              <button
                onClick={closeDomainHelpModal}
                className="text-slate-400 hover:text-slate-600"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-6 w-6"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>
            <div className="flex justify-center">
              <img
                src={domainhelpimg}
                alt="Jira 도메인 URL 안내"
                className="max-w-full h-auto"
              />
            </div>
          </div>
        </div>
      )}

      {/* 이메일 도움말 이미지 모달 */}
      {isEmailHelpModalOpen && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-[60]">
          <div className="bg-white rounded-lg p-6 w-auto max-w-[90vw] max-h-[90vh] overflow-auto">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-[16px] font-[paperlogy6]">
                Jira 사용자 이메일 안내
              </h3>
              <button
                onClick={closeEmailHelpModal}
                className="text-slate-400 hover:text-slate-600"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-6 w-6"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>
            <div className="flex justify-center">
              <img
                src={useremailhelpimg}
                alt="Jira 사용자 이메일 안내"
                className="max-w-full h-auto"
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AlarmSetting;
