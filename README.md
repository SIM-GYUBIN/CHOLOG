<img width="1769" height="886" alt="image" src="https://github.com/user-attachments/assets/7e8abb14-60f3-4a64-be6a-2a455288704a" />


<br/>

<img src="https://github.com/user-attachments/assets/ebb5652b-cc41-4b19-a535-d4cb9fe2da2a" alt="cholog" align="right" height="100" />

<br/>
<br/>

# CHO:LOG

![버전](https://img.shields.io/badge/version-1.0.0-blue?style=flat-square)
![라이센스](https://img.shields.io/badge/license-MIT-green?style=flat-square)
![npm](https://img.shields.io/badge/npm-green?style=flat-square&logo=npm&logoColor=white)

<br />

**✍️ 초보자를 위한 로그 관리**

‘CHO:LOG’는 초보 개발자에게 가장 쉽고 똑똑한 로그 관리 방법을 제공합니다.

JS 혹은 TS 기반의 Frontend와 Springboot 기반 Backend에 SDK를 설치하여 프론트엔드-백엔드간 강력한 통합 로그 관리를 시작해보세요.

## 📚 Quick Link
- #### 🏠 [Service Link](https://www.cholog.com/)
- #### 🦎 [FE Library(NPM)](https://www.npmjs.com/package/cholog-sdk)
- #### 🗿 [BE(Springboot) Dependency](https://github.com/SIM-GYUBIN/CHOLOG/tree/sdk-dev/sdk/java)

## 💁‍♂️ Introduction

  <blockquote>
    <h3> 삼성청년SW아카데미 12기 자율프로젝트 우수상(지역) 수상작 🎉</h3>
  </blockquote>

<img width="1946" height="1154" alt="image" src="https://github.com/user-attachments/assets/839a7ad3-42e3-4c89-80b1-c68c0a4ae012" />

### 간편 SDK 설치
<img width="2416" height="921" alt="image" src="https://github.com/user-attachments/assets/cb84ad83-a577-4e4d-bc40-af8ac6d1db60" />

> 쉬운 SDK 설치로 빠르게 시작할 수 있습니다

### SDK 제공 기능

<img width="1000" alt="image" src="https://github.com/user-attachments/assets/7af37206-81a4-491c-b755-c7071248a821" />
<img width="1000" alt="image" src="https://github.com/user-attachments/assets/d25b3f5d-9fe7-44c1-8b7d-9445e258db3c" />
<img width="1000" alt="image" src="https://github.com/user-attachments/assets/418558ad-2994-4987-aac2-8ef146805e46" />

> **Javascript SDK (FE) 기능
>
> - 자동 콘솔 로그 수집: console.log, console.info, console.warn, console.error, console.debug, console.trace를 통해 출력되는 모든 로그를 자동으로 감지하고 수집합니다.
>
> - 네트워크 요청 자동 로깅: Workspace API와 XMLHttpRequest를 통해 발생하는 모든 네트워크 요청과 응답(성공/실패, 상태 코드, 소요 시간 등)을 자동으로 로깅합니다.
>
> - 분산 추적 지원 (Trace ID): 각 네트워크 요청 헤더에 X-Request-ID (Trace ID)를 자동으로 주입하고, 관련 로그에 동일한 Trace ID를 기록하여 요청의 흐름과 관련 로그를 쉽게 추적할 수 있도록 합니다.
>
> - 간편한 커스텀 로그 API: Cholog.info(), Cholog.warn(), Cholog.error() 등 직관적인 API를 통해 원하는 시점에 커스텀 로그를 손쉽게 전송할 수 있습니다.
>
> - 자동 에러 감지 (ErrorCatcher): 전역 에러(window.onerror, unhandledrejection)를 감지하여 자동으로 로그를 전송합니다. (구현에 따라) 사용자 이벤트 추적 (EventTracker): 특정 DOM 요소의 클릭과 같은 사용자 상호작용 이벤트를 추적할 수 있습니다. (구현에 따라)
>
> - 효율적인 로그 전송: 수집된 로그는 즉시 전송되지 않고, 내부 큐에 쌓여 정해진 간격(기본 1초) 또는 큐 크기(기본 100KB)에 따라 일괄(Batch) 전송되어 네트워크 부하를 최소화합니다.
>

> **Springboot SDK (BE) 기능
>
> - 

### 통합 로그관리
<img width="2013" height="1120" alt="image" src="https://github.com/user-attachments/assets/307c8fbc-d54e-4029-a4be-4553567ca356" />
<img width="1919" height="1129" alt="image" src="https://github.com/user-attachments/assets/1deb7788-d7b8-404e-8df2-7b99feb3b0db" />

> 서비스의 로그 발생 추이를 확인하세요
> 
> 키워드 혹은 API단위로 조회가 가능합니다
>
> 소스(FE 혹은 BE), 로그 타입을 필터링하여 조회할 수 있습니다  

### 개별 로그 조회
<img width="2019" height="1107" alt="image" src="https://github.com/user-attachments/assets/47894ecd-9239-4924-bf5e-c503f3436e15" />

> 로그의 상세정보를 확인하세요
>
> 화면의 어떤 요소 클릭으로 부터, 어떤 API 호출, 어떤 서버 로그까지 연관되어 있는지 알 수 있습니다
>
> 어려운 로그 내용, 복잡한 stacktrace가 고민이라면, AI 분석을 바로 요청할 수 있습니다

### 협업 도구
<img width="1984" height="1111" alt="image" src="https://github.com/user-attachments/assets/1cbe886f-d461-4078-9a09-936c7d1b6ea7" />
<img width="2002" height="1034" alt="image" src="https://github.com/user-attachments/assets/640caf8e-615d-4ca3-b3a9-d0703002aa47" />

> Jira 이슈를 바로 등록해보세요
>
> 특정 키워드가 포함된 로그 발생에 대한 알림을 Mattermost로 바로 받을 수 있어요
>
> 민첩한 로그 대응으로, 서비스를 보다 사용자 친화적으로 운영할 수 있어요

## 🧩 Architecture
<img width="70%" alt="image" src="https://github.com/user-attachments/assets/1f1575d4-b3fb-4615-89a7-68401102caf1" />

웹서비스 인스턴스와 로그 수집 및 관리 인스턴스로 운영되었습니다.


## 👥 Team 초록
