import React, { useState, useEffect, ChangeEvent, FormEvent } from "react";
import logo from "../assets/logo.svg";
import close from "../assets/delete.svg";
import { useDispatch, useSelector } from "react-redux";
import { AppDispatch, RootState } from "../store/store";
import {
  userLogin,
  userSignup,
  resetLoginStatus,
} from "../store/slices/userSlice";
import { useNavigate } from "react-router-dom";

type MatchStatus = "match" | "mismatch" | null;

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch<AppDispatch>();
  const { isLoading, error, signupSuccess } = useSelector(
    (state: RootState) => state.user
  );

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isLoginValid, setIsLoginValid] = useState(false);
  const [showSignupModal, setShowSignupModal] = useState(false);

  // 회원가입 상태
  const [signupEmail, setSignupEmail] = useState("");
  const [signupPassword, setSignupPassword] = useState("");
  const [signupConfirmPassword, setSignupConfirmPassword] = useState("");
  const [signupNickname, setSignupNickname] = useState("");
  const [signupMatchStatus, setSignupMatchStatus] = useState<MatchStatus>(null);

  //SSAFY 로그인
  const SSAFY_CLIENT_ID = "19615ce9-e7d0-4696-aacd-ef1de95b291e";
  const SSAFY_REDIRECT_URI = "https://www.cholog.com/api/user/login/ssafy";

  const handleSsafyLogin = () => {
    const ssoUrl = `https://project.ssafy.com/oauth/sso-check?client_id=${SSAFY_CLIENT_ID}&redirect_uri=${SSAFY_REDIRECT_URI}&response_type=code`;
    window.location.href = ssoUrl;
  };

  const { isLoggedIn } = useSelector((state: RootState) => state.user);

  // 로그인 성공 시 이동
  useEffect(() => {
    if (isLoggedIn) {
      navigate("/projectList");
      dispatch(resetLoginStatus());
    }
  }, [isLoggedIn, navigate, dispatch]);

  // 로그인 유효성
  useEffect(() => {
    setIsLoginValid(email !== "" && password !== "");
  }, [email, password]);

  // 회원가입 비밀번호 일치 확인
  useEffect(() => {
    if (signupConfirmPassword === "") {
      setSignupMatchStatus(null);
    } else if (signupPassword === signupConfirmPassword) {
      setSignupMatchStatus("match");
    } else {
      setSignupMatchStatus("mismatch");
    }
  }, [signupPassword, signupConfirmPassword]);

  useEffect(() => {
    if (signupSuccess) {
      alert("회원가입 성공!");
      setShowSignupModal(false);
    }
  }, [signupSuccess]);

  // 로그인 핸들러
  const handleLogin = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!isLoginValid) return;
    dispatch(userLogin({ email, password }));
  };

  // 회원가입 핸들러
  const handleSignup = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!signupEmail || !signupPassword || !signupConfirmPassword) {
      alert("모든 필드를 입력해주세요.");
      return;
    }

    if (signupPassword !== signupConfirmPassword) {
      alert("비밀번호가 일치하지 않습니다.");
      return;
    }

    dispatch(
      userSignup({
        email: signupEmail,
        password: signupPassword,
        nickname: signupNickname,
      })
    );
  };

  return (
    <>
      {/* 회원가입 모달 */}
      {showSignupModal && (
        <div className="fixed inset-0 flex items-center justify-center bg-black/25 bg-opacity-40 z-50">
          <div className="bg-[var(--bg)] w-full max-w-sm p-10 rounded-lg shadow-lg relative">
            {/* 닫기 버튼 */}
            <button
              type="button"
              onClick={() => setShowSignupModal(false)}
              className="absolute p-2 top-4 right-4"
            >
              <img src={close} alt="닫기" className="w-3 h-3" />
            </button>
            <h2 className="text-lg font-semibold mb-6">회원가입</h2>
            <form onSubmit={handleSignup} className="flex flex-col gap-4">
              {/* 닉네임 */}
              <div className="flex flex-col">
                <label className="text-sm text-[var(--text)] text-left mb-0.5">
                  닉네임
                </label>
                <input
                  type="text"
                  placeholder="이름을 입력해주세요"
                  value={signupNickname}
                  onChange={(e) => setSignupNickname(e.target.value)}
                  className="caret-lime-500 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-lime-400 placeholder:text-sm placeholder:text-gray-300"
                />
              </div>

              {/* 이메일 */}
              <div className="flex flex-col">
                <label className="text-sm text-[var(--text)] text-left mb-0.5">
                  이메일
                </label>
                <input
                  type="email"
                  placeholder="example@cholog.com"
                  value={signupEmail}
                  onChange={(e) => setSignupEmail(e.target.value)}
                  className="caret-lime-500 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-lime-400 placeholder:text-sm placeholder:text-gray-300"
                />
              </div>

              {/* 비밀번호 */}
              <div className="flex flex-col">
                <label className="text-sm text-gray-700 text-left mb-0.5">
                  비밀번호
                </label>
                <input
                  type="password"
                  placeholder="영문, 숫자 포함 8자 이상 추천"
                  value={signupPassword}
                  onChange={(e) => setSignupPassword(e.target.value)}
                  className="caret-lime-500 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-lime-400 placeholder:text-sm placeholder:text-gray-300"
                />
              </div>

              {/* 비밀번호 확인 */}
              <div className="flex flex-col">
                <label className="text-sm text-gray-700 text-left mb-0.5">
                  비밀번호 확인
                </label>
                <input
                  type="password"
                  placeholder="비밀번호를 한 번 더 입력해주세요"
                  value={signupConfirmPassword}
                  onChange={(e) => setSignupConfirmPassword(e.target.value)}
                  className="caret-lime-500 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-lime-400 placeholder:text-sm placeholder:text-gray-300"
                />
              </div>

              {/* 비밀번호 일치 여부 메시지 */}
              {signupMatchStatus === "match" && (
                <p className="text-sm text-lime-600">비밀번호가 일치합니다.</p>
              )}
              {signupMatchStatus === "mismatch" && (
                <p className="text-sm text-red-500">
                  비밀번호가 일치하지 않습니다.
                </p>
              )}
              {error?.code && (
                <p className="text-sm text-red-500 mt-1">{error.message}</p>
              )}

              {/* 회원가입 버튼 */}
              <button
                type="submit"
                className="w-full py-2 mt-3 rounded-md bg-lime-500 text-white hover:bg-lime-600 transition-colors"
              >
                회원가입
              </button>
            </form>
          </div>
        </div>
      )}

      {/* 로그인 폼 */}
      <div className="min-h-[700px] bg-[var(--bg)] flex flex-col items-center px-4 py-8">
        <div className="flex flex-col items-center w-full max-w-md px-6">
          <img src={logo} alt="cho:log*" className="w-50 mb-8" />

          <form
            onSubmit={handleLogin}
            className="w-full max-w-xs flex flex-col gap-3"
          >
            <input
              type="email"
              placeholder="이메일"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="caret-lime-500 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-lime-400 placeholder:text-gray-500"
            />
            <input
              type="password"
              placeholder="비밀번호"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="caret-lime-500 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-lime-400 placeholder:text-gray-500"
            />

            {error?.code && (
              <p className="text-sm text-red-500 mt-1">{error.message}</p>
            )}

            <button
              type="submit"
              disabled={!isLoginValid}
              className={`w-full mt-1
                
                 py-2 rounded-md transition-colors ${
                   isLoginValid
                     ? "bg-lime-500 text-white hover:bg-lime-600"
                     : "bg-gray-300 text-gray-500 cursor-not-allowed"
                 }`}
            >
              로그인
            </button>

            {/* 다른 방법 로그인 버튼 */}
            <button
              type="button"
              onClick={handleSsafyLogin}
              className="w-full py-2 mt-2 border border-gray-400 text-[var(--text)] rounded-md bg-[var(--bg)] hover:bg-blue-50 transition-colors hover:border-blue-400"
            >
              ssafy 로그인
            </button>

            {/* 회원가입 버튼 */}
            <div className="w-full flex justify-end mb-2">
              <button
                type="button"
                onClick={() => setShowSignupModal(true)}
                className="text-xs text-lime-600 underline hover:text-lime-800"
              >
                회원가입
              </button>
            </div>
          </form>
        </div>
      </div>
    </>
  );
};

export default LoginPage;
