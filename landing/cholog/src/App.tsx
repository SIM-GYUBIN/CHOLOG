import React, { useState } from "react";
import { BrowserRouter } from "react-router-dom";
import FullpageScroll from "./FullPageScroll";

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
      <div className="bg-white border border-[#C5E1A5] text-[#2E7D32] font-mono text-sm p-4 rounded-lg shadow-sm whitespace-pre-wrap">
        {code}
      </div>
    </div>
  );
}

function AppContent() {
  return (
    <FullpageScroll>
      {/* Section 1: Hero */}
      <FullpageScroll.Section className="bg-white text-black flex flex-col items-center justify-center px-6">
        <h1 className="text-[5rem] md:text-[7rem] font-extrabold leading-tight text-center tracking-tighter">
          <span className="text-[#5EA500]">초</span>보자를 위한
          <br />
          <span className="text-[#5EA500]">로그</span>관리 서비스
        </h1>
        <p className="mt-6 text-xl text-gray-600 max-w-xl text-center">
          <span className="text-[#5EA500] font-semibold">CHO:LOG</span>을 로그
          관리가 막막한 초보 개발자에게
          <br /> 가장 쉽고 똑똑한 로그 관리 방법을 제공합니다.
        </p>
      </FullpageScroll.Section>

      {/* Section 2: 프론트 설치 */}
      {/* Section 2: 프론트에서도 CHO:LOG를! */}
      <FullpageScroll.Section className="bg-[#F4FCE3] text-black flex flex-col items-center justify-center px-6 py-20">
        <h2 className="text-[2.5rem] md:text-[3rem] font-bold mb-6 text-[#5EA500] text-center">
          🔧 프론트에서도 CHO:LOG를!
        </h2>
        <p className="text-lg text-gray-700 text-center mb-10 max-w-2xl">
          프론트엔드에서도 단 1줄 설치로
          <br />
          로그 수집 SDK를 바로 사용할 수 있어요.
        </p>

        <div className="w-full max-w-3xl mb-10">
          <h3 className="text-xl font-semibold mb-2 text-[#4A8400]">
            ✅ 터미널 설치
          </h3>
          <CopyableCode code={`npm install cholog-sdk`} />
          <p className="text-sm text-gray-500 mt-2">
            ※ React, Vue, Vite, Next.js 등 대부분의 프레임워크 지원
          </p>
        </div>

        <div className="w-full max-w-3xl">
          <h3 className="text-xl font-semibold mb-2 text-[#4A8400]">
            ✅ main.tsx에 초기화 코드 추가
          </h3>
          <CopyableCode
            code={`import Cholog from "cholog-sdk";

Cholog.init({
  apiKey: "f98c78f3-8bea-4ead-a631-3d79bae44c11",
  environment: "prod",
});`}
          />
          <p className="text-sm text-gray-500 mt-2">
            ※ 개발 환경에서는 <code>environment: "dev"</code>로 설정해도 됩니다.
          </p>
        </div>
      </FullpageScroll.Section>

      {/* Section 3: 백엔드 설치 */}
      <FullpageScroll.Section className="bg-[#ECFBD9] text-black flex flex-col items-center justify-center px-6 py-20">
        <h2 className="text-[2.5rem] md:text-[3rem] font-bold mb-6 text-[#5EA500] text-center">
          ☘️ 백엔드에서도 CHO:LOG를!
        </h2>
        <p className="text-lg text-gray-700 text-center mb-10 max-w-2xl">
          Gradle 의존성 추가와 간단한 YML 설정만으로 <br />
          백엔드 에러 로그도 자동으로 수집할 수 있어요.
        </p>

        {/* Gradle 설정 */}
        <div className="w-full max-w-3xl mb-8">
          <h3 className="text-xl font-semibold mb-2 text-[#4A8400]">
            1️⃣ Gradle 설정
          </h3>
          <CopyableCode
            code={`repositories {
  mavenCentral()
  maven { url 'https://jitpack.io' }
}

dependencies {
  implementation 'com.ssafy.lab.s12-final:S12P31B207:v1.0.9'
}`}
          />
        </div>

        {/* YML 설정 */}
        <div className="w-full max-w-3xl">
          <h3 className="text-xl font-semibold mb-2 text-[#4A8400]">
            2️⃣ application.yml 설정
          </h3>
          <CopyableCode
            code={`cholog:
  logger:
    api-key: your-api-key`}
          />
          <p className="text-sm text-gray-500 mt-2">
            ※ <code>url</code>은 로그 수신 서버, <code>api-key</code>는 사용자
            고유 키입니다.
          </p>
        </div>
      </FullpageScroll.Section>

      {/* Section 4: 기능 소개 */}
      {/* Section 4: 왜 CHO:LOG일까요? */}
      <FullpageScroll.Section className="bg-white text-black flex flex-col items-center justify-center px-6 py-20 text-center">
        <h2 className="text-[3rem] font-bold mb-16 text-[#5EA500]">
          왜 <span className="font-extrabold">CHO:LOG</span>일까요?
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-10 max-w-6xl w-full">
          {/* 기능 1 */}
          <div className="flex flex-col items-center px-4">
            <div className="text-[3rem] mb-4">🔍</div>
            <h3 className="text-xl font-semibold mb-2">
              에러의 원인, 바로 찾기
            </h3>
            <p className="text-gray-500 text-sm">
              로그 수준 · 위치 · 시간 순으로 정리되어
              <br />
              디버깅 시간을 획기적으로 줄여줍니다.
            </p>
          </div>

          {/* 기능 2 */}
          <div className="flex flex-col items-center px-4">
            <div className="text-[3rem] mb-4">🤖</div>
            <h3 className="text-xl font-semibold mb-2">AI 로그 분석</h3>
            <p className="text-gray-500 text-sm">
              AI가 중요한 로그만 골라서 분석해주니
              <br />
              놓치던 이슈도 놓치지 않아요.
            </p>
          </div>

          {/* 기능 3 */}
          <div className="flex flex-col items-center px-4">
            <div className="text-[3rem] mb-4">⚡</div>
            <h3 className="text-xl font-semibold mb-2">1분 설치, 바로 연동</h3>
            <p className="text-gray-500 text-sm">
              프론트·백엔드 어디든 SDK 간편한 설치로
              <br />
              로그 수집이 시작됩니다.
            </p>
          </div>
        </div>
      </FullpageScroll.Section>

      {/* Section 5: CTA */}
      <FullpageScroll.Section className="bg-[#5EA500] text-white flex flex-col items-center justify-center px-6 text-center">
        <h2 className="text-[3.5rem] font-extrabold mb-4">
          서비스 이용하러 가기
        </h2>
        <p className="text-lg mb-6 max-w-xl">
          로그, 아직도 콘솔에서 보고 있나요?
          <br />
          Cho:log는 <strong className="text-white">직관적 대시보드</strong>로
          로그를 쉽게 보여줍니다.
        </p>
        <button
          onClick={() => (window.location.href = "https://www.cholog.com")}
          className="px-10 py-4 bg-white text-[#5EA500] rounded-2xl text-lg font-semibold hover:bg-gray-100 transition"
        >
          지금 시작하기 →
        </button>
      </FullpageScroll.Section>
    </FullpageScroll>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
}

export default App;
