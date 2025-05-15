import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { AppDispatch, RootState } from '../store/store';
import { saveWebhook, updateWebhook } from '../store/slices/webhookSlice';
import URLGuideModal from './JiraGuideModal';
import { useParams } from 'react-router-dom';

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
  const [mmURL, setMmURL] = useState('');
  const [keywords, setKeywords] = useState('');
  const [notificationENV, setNotificationENV] = useState('prod'); // 기본값 설정
  const [isEnabled, setIsEnabled] = useState(true);
  const [isURLGuideOpen, setIsURLGuideOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<'webhook' | 'jira'>('webhook');
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  // 폼 유효성 검사 상태
  const [isFormValid, setIsFormValid] = useState(false);
  const [errors, setErrors] = useState({
    mmURL: '',
    keywords: ''
  });

  // webhookData가 변경될 때 폼 데이터 업데이트 및 콘솔 로그 출력
  useEffect(() => {
    // API 응답 결과를 콘솔에 출력
    console.log('웹훅 설정 API 응답 결과:', webhookData);
    
    if (webhookData?.exists && webhookData.webhookItem) {
      setMmURL(webhookData.webhookItem.mmURL || '');
      setKeywords(webhookData.webhookItem.keywords || '');
      setNotificationENV(webhookData.webhookItem.notificationENV || 'prod');
      setIsEnabled(webhookData.webhookItem.isEnabled ?? true);
    } else {
      // exists가 false인 경우 초기화
      setMmURL('');
      setKeywords('');
      setNotificationENV('prod');
      setIsEnabled(true);
    }
  }, [webhookData]);

  // 폼 유효성 검사
  useEffect(() => {
    const validateForm = () => {
      const newErrors = {
        mmURL: '',
        keywords: ''
      };
      
      // URL 유효성 검사
      if (!mmURL) {
        newErrors.mmURL = 'Mattermost URL을 입력해주세요';
      } else if (!mmURL.startsWith('https://')) {
        newErrors.mmURL = 'URL은 https://로 시작해야 합니다';
      }
      
      // 키워드 유효성 검사
      if (!keywords) {
        newErrors.keywords = '알림 받을 키워드를 입력해주세요';
      }
      
      setErrors(newErrors);
      
      // 모든 필수 필드가 유효한지 확인
      setIsFormValid(!newErrors.mmURL && !newErrors.keywords);
    };
    
    validateForm();
  }, [mmURL, keywords]);

  // isOpen이 false에서 true로 변경될 때 webhook 탭으로 초기화
  useEffect(() => {
    if (isOpen) {
      setActiveTab('webhook');
    }
  }, [isOpen]);

  // 폼 제출 핸들러
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
        await dispatch(updateWebhook({
          projectId: Number(projectId),
          webhookItem
        })).unwrap();
        console.log('웹훅 설정이 성공적으로 수정되었습니다.');
      } else {
        // 웹훅 데이터가 존재하지 않으면 POST 요청
        await dispatch(saveWebhook({
          projectId: Number(projectId),
          webhookItem
        })).unwrap();
        console.log('웹훅 설정이 성공적으로 생성되었습니다.');
      }
      
      // 성공 시 모달 닫기
      onClose();
    } catch (error) {
      console.error('웹훅 설정 저장 중 오류가 발생했습니다:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg flex w-[700px] text-start text-slate-700">
        {/* 사이드바 */}
        <div className="w-[200px] bg-slate-50 p-4 rounded-l-lg border-r border-slate-200">
          <div className="text-[18px] font-[paperlogy6] mb-6">설정</div>
          <div className="space-y-2">
            <button
              onClick={() => setActiveTab('webhook')}
              className={`w-full text-start p-2 rounded-lg text-[14px] transition-colors ${
                activeTab === 'webhook' 
                ? 'bg-lime-500/10 text-slate-800 font-[paperlogy6]' 
                : 'hover:bg-slate-100'
              }`}
            >
              Webhook 설정
            </button>
            <button
              onClick={() => setActiveTab('jira')}
              className={`w-full text-start p-2 rounded-lg text-[14px] transition-colors ${
                activeTab === 'jira' 
                ? 'bg-lime-500/10 text-slate-800 font-[paperlogy6]' 
                : 'hover:bg-slate-100'
              }`}
            >
              Jira 연동
            </button>
          </div>
        </div>

        {/* 메인 컨텐츠 */}
        <div className="flex-1 p-6">
          {activeTab === 'webhook' ? (
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
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="darkgray" className="w-4 h-4">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M9.879 7.519c1.171-1.025 3.071-1.025 4.242 0 1.172 1.025 1.172 2.687 0 3.712-.203.179-.43.326-.67.442-.745.361-1.45.999-1.45 1.827v.75M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9 5.25h.008v.008H12v-.008z" />
                      </svg>
                    </button>
                  </div>
                  <input
                    type="text"
                    value={mmURL}
                    onChange={(e) => setMmURL(e.target.value)}
                    placeholder="https://meeting.ssafy.com/hooks/$$URL_ADDRESS"
                    className={`w-full px-3 py-2 border rounded-lg ${errors.mmURL ? 'border-red-500' : 'border-slate-700'} text-[12px]`}
                    required
                  />
                  {errors.mmURL && (
                    <p className="text-red-500 text-[11px] mt-1">{errors.mmURL}</p>
                  )}
                </div>

                <div>
                  <div className="text-[14px] mb-1">로그 키워드<span className="text-red-500">*</span></div>
                  <div className="mb-2 text-[12px] text-slate-500 font-[paperlogy4]">웹훅 알림을 받고싶은 로그 키워드를 입력해주세요</div>
                  {/* 키워드 api요청할때 ","포함해서 그냥 텍스트 자체로 보내기!! */}
                  <input
                    type="text"
                    value={keywords}
                    onChange={(e) => setKeywords(e.target.value)}
                    placeholder="Timeout, Unauthorized, Not_found ..."
                    className={`w-full px-3 py-2 border rounded-lg ${errors.keywords ? 'border-red-500' : 'border-slate-700'} text-[12px]`}
                    required
                  />
                  {errors.keywords && (
                    <p className="text-red-500 text-[11px] mt-1">{errors.keywords}</p>
                  )}
                  <p className="text-[11px] text-slate-500 mt-1">쉼표(,)로 구분하여 여러 키워드를 입력할 수 있습니다</p>
                </div>

                <div>
                  <div className="text-[14px] mb-2">알림 받을 개발 환경</div>
                  <select
                    value={notificationENV}
                    onChange={(e) => setNotificationENV(e.target.value)}
                    className="w-full px-3 py-2 border rounded-lg border-slate-700 text-[12px]"
                  >
                    <option value="prod">배포 환경</option>
                    <option value="local">로컬 환경</option>
                  </select>
                </div>

                <div>
                  <label className="flex items-center gap-5">
                    <div className="text-sm">알림 활성화 <span className="text-red-500">*</span></div>
                    <label className="relative inline-block w-9 h-6 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={isEnabled}
                        onChange={(e) => setIsEnabled(e.target.checked)}
                        className="sr-only peer"
                        required
                      />
                      <div className="absolute inset-0 bg-gray-300 dark:bg-gray-100 rounded-full transition-colors peer-checked:bg-lime-600"></div>
                      <div className="absolute left-0.5 top-0.5 w-5 h-5 bg-white dark:bg-gray-50 rounded-full flex items-center justify-center text-[11px] text-lime-700 transition-transform duration-300 peer-checked:translate-x-3">
                        {isEnabled ? "♪" : ""}
                      </div>
                    </label>
                  </label>
                </div>
              </div>
            </>
          ) : (
            <>
              <div className="text-[18px] font-[paperlogy6] mb-4">Jira 연동</div>
              <div className="space-y-4">
                <div className="text-[14px] text-slate-500">
                  Jira 연동 기능은 준비 중입니다.
                </div>
              </div>
            </>
          )}

          <div className="flex justify-end gap-2 mt-6">
            <button
              onClick={onClose}
              className="text-[12px] px-4 text-gray-600 hover:text-gray-800"
              disabled={isSubmitting}
            >
              취소
            </button>
            {activeTab === 'webhook' && (
              <button
                onClick={handleSubmit}
                disabled={!isFormValid || isSubmitting}
                className={`text-[12px] px-3 py-2 ${isFormValid && !isSubmitting ? 'bg-lime-600 hover:bg-lime-700' : 'bg-lime-300 cursor-not-allowed'} text-white rounded-lg transition-colors`}
              >
                {isSubmitting ? '처리 중...' : webhookData?.exists ? '수정하기' : '등록하기'}
              </button>
            )}
          </div>
        </div>
      </div>
      <URLGuideModal 
        isOpen={isURLGuideOpen}
        onClose={() => setIsURLGuideOpen(false)}
      />
    </div>
  );
};

export default AlarmSetting;