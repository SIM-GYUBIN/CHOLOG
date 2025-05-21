import { useState } from "react";
import { useNavigate } from "react-router-dom";
import FullpageScroll from "../components/FullpageScroll";

// 복사 가능한 코드 블록 컴포넌트
function CopyableCode({ code }: { code: string }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="relative w-full">
      <button
        onClick={handleCopy}
        className="absolute top-3 right-3 text-sm bg-[#5EA500] text-white px-3 py-1 rounded hover:bg-[#4A8400] transition"
      >
        {copied ? "Copied!" : "Copy"}
      </button>
      <div className="bg-white border border-[#C5E1A5] text-[#2E7D32] font-mono text-sm p-4 rounded-lg shadow-sm whitespace-pre-wrap text-left">
        {code}
      </div>
    </div>
  );
}

function GuidePage() {
  const navigate = useNavigate();

  return (
    <div className="relative">
      <button
        onClick={() => navigate("/projectlist")}
        className="fixed top-8 right-8 z-50 px-6 py-2 bg-[#5EA500] text-white rounded-lg hover:bg-[#4A8400] transition-colors"
      >
        프로젝트 목록으로 돌아가기
      </button>

      <FullpageScroll>
        {/* 프론트엔드 설치 가이드 */}
        <FullpageScroll.Section className="bg-[#F4FCE3] text-black flex flex-col items-center justify-center px-6 py-20">
          <h2 className="text-[2.5rem] md:text-[3rem] font-bold mb-6 text-[#5EA500] text-center">
            ☘️ 프론트엔드 설치 가이드
          </h2>
          <div className="w-full max-w-3xl space-y-8">
            <div>
              <h3 className="text-xl font-semibold mb-4 text-[#4A8400]">
                ✅ 터미널 설치
              </h3>
              <CopyableCode code={`npm install cholog-sdk`} />
              <p className="text-sm text-gray-500 mt-2">
                ※ React, Vue, Vite, Next.js 등 대부분의 프레임워크 지원
              </p>
            </div>

            <div>
              <h3 className="text-xl font-semibold mb-4 text-[#4A8400]">
                ✅ main.tsx에 초기화 코드 추가
              </h3>
              <CopyableCode
                  code={`import Cholog from "cholog-sdk";

Cholog.init({
  apiKey: "YOUR_API_KEY",
  environment: "prod",
});`}
              />
              <p className="text-sm text-gray-500 mt-2">
                ※ 개발 환경에서는 environment: "dev"로 설정해도 됩니다.
              </p>
              <p className="text-sm text-gray-500 mt-2">
                ※ api-key는 프로젝트 고유 키입니다. 발급된 ProjectID를 입력해주세요!
              </p>
            </div>
          </div>
        </FullpageScroll.Section>

        {/* 백엔드 설치 가이드 */}
        <FullpageScroll.Section className="bg-[#ECFBD9] text-black flex flex-col items-center justify-center px-6 py-20">
          <h2 className="text-[2.5rem] md:text-[3rem] font-bold mb-6 text-[#5EA500] text-center">
            ☘️ 백엔드 설치 가이드
          </h2>
          <div className="w-full max-w-3xl space-y-8">
            <div>
              <h3 className="text-xl font-semibold mb-4 text-[#4A8400]">
                ✅ Gradle 설정
              </h3>
              <CopyableCode
                code={`repositories {
  mavenCentral()
  maven { url 'https://jitpack.io' }
}

dependencies {
  implementation 'com.ssafy.lab.s12-final:S12P31B207:v1.1.0'
}`}
              />
            </div>

            <div>
              <h3 className="text-xl font-semibold mb-4 text-[#4A8400]">
                ✅ application.yml 설정
              </h3>
              <CopyableCode
                  code={`cholog:
  logger:
    api-key: YOUR_API_KEY
    cors-enabled: true`}
              />
              <p className="text-sm text-gray-500 mt-2">
                ※ api-key는 프로젝트 고유 키입니다. 발급된 ProjectID를 입력해주세요!
              </p>
            </div>
          </div>
        </FullpageScroll.Section>
      </FullpageScroll>
    </div>
  );
}

export default GuidePage;
