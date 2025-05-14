import React, { useState, useEffect } from 'react';
import URLGuideModal from './URLGuideModal';

interface AlarmSettingProps {
  isOpen: boolean;
  onClose: () => void;
  webhookData?: {
    exists: boolean;
    webhookItem?: {
      id: number;
      mmURL: string;
      logLevel: string;
      notificationENV: string;
      isEnabled: boolean;
    };
  };
  onSave: (data: any) => void;
}

const AlarmSetting: React.FC<AlarmSettingProps> = ({
  isOpen,
  onClose,
  webhookData,
  onSave,
}) => {
  const [mmURL, setMmURL] = useState(webhookData?.webhookItem?.mmURL || '');
  const [logLevel, setLogLevel] = useState(webhookData?.webhookItem?.logLevel || '');
  const [notificationENV, setNotificationENV] = useState(webhookData?.webhookItem?.notificationENV || '');
  const [isEnabled, setIsEnabled] = useState(webhookData?.webhookItem?.isEnabled ?? true);
  const [isURLGuideOpen, setIsURLGuideOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<'webhook' | 'jira'>('webhook');

  // isOpen이 false에서 true로 변경될 때 webhook 탭으로 초기화
  useEffect(() => {
    if (isOpen) {
      setActiveTab('webhook');
    }
  }, [isOpen]);

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
                    className="w-full px-3 py-2 border rounded-lg border-slate-700 text-[12px]"
                    required
                  />
                </div>

                <div>
                  <div className="text-[14px] mb-1">로그 키워드<span className="text-red-500">*</span></div>
                  <div className="mb-2 text-[12px] text-slate-500 font-[paperlogy4]">웹훅 알림을 받고싶은 로그 키워드를 입력해주세요</div>
                  {/* 키워드 api요청할때 ","포함해서 그냥 텍스트 자체로 보내기!! */}
                  <input
                    type="text"
                    value={logLevel}
                    onChange={(e) => setLogLevel(e.target.value)}
                    placeholder="Timeout, Unauthorized, Not_found ..."
                    className="w-full px-3 py-2 border rounded-lg  border-slate-700 text-[12px]"
                    required
                  />
                </div>

                <div>
                  <div className="text-[14px] mb-2">알림 받을 개발 환경</div>
                  <select
                    value={notificationENV}
                    onChange={(e) => setNotificationENV(e.target.value)}
                    className="w-full px-3 py-2 border rounded-lg  border-slate-700 text-[12px]"
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
            >
              취소
            </button>
            {activeTab === 'webhook' && (
              <button
                onClick={() => {
                  onSave({
                    webhookItem: {
                      mmURL,
                      logLevel,
                      notificationENV,
                      isEnabled,
                    },
                  });
                  onClose();
                }}
                className="text-[12px] px-3 py-2 bg-lime-600 text-white rounded-lg hover:bg-lime-700"
              >
                등록하기
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