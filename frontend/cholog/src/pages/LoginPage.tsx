import React, { useState, useEffect, ChangeEvent, FormEvent } from "react";
import logo from "../assets/logo.svg";

type MatchStatus = "match" | "mismatch" | null;

const LoginPage: React.FC = () => {
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [confirmPassword, setConfirmPassword] = useState<string>("");
  const [matchStatus, setMatchStatus] = useState<MatchStatus>(null);
  const [isFormValid, setIsFormValid] = useState<boolean>(false);

  useEffect(() => {
    if (confirmPassword === "") {
      setMatchStatus(null);
    } else if (password === confirmPassword) {
      setMatchStatus("match");
    } else {
      setMatchStatus("mismatch");
    }
  }, [password, confirmPassword]);

  useEffect(() => {
    const allFilled = email !== "" && password !== "" && confirmPassword !== "";
    const matched = password === confirmPassword;
    setIsFormValid(allFilled && matched);
  }, [email, password, confirmPassword]);

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!email || !password || !confirmPassword) {
      alert("모든 필드를 입력해주세요.");
      return;
    }

    if (matchStatus !== "match") {
      alert("비밀번호가 일치하지 않습니다.");
      return;
    }

    alert("로그인 성공!");
  };

  const handleEmailChange = (e: ChangeEvent<HTMLInputElement>) =>
    setEmail(e.target.value);
  const handlePasswordChange = (e: ChangeEvent<HTMLInputElement>) =>
    setPassword(e.target.value);
  const handleConfirmChange = (e: ChangeEvent<HTMLInputElement>) =>
    setConfirmPassword(e.target.value);

  return (
    <div className="min-h-[700px] bg-white flex flex-col items-center px-4 py-8">
      <div className="flex flex-col items-center w-full max-w-md px-6">
        {/* 로고 */}
        <img src={logo} alt="cho:log*" className="w-50 mb-8" />

        {/* 로그인 폼 */}
        <form
          onSubmit={handleSubmit}
          className="w-full max-w-xs flex flex-col gap-3"
        >
          {/* 이메일 */}
          <div className="flex flex-col mb-1">
            <label
              htmlFor="email"
              className="text-sm text-gray-700 text-left mb-1"
            />
            <input
              type="email"
              placeholder="email"
              value={email}
              onChange={handleEmailChange}
              className="caret-lime-500 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-lime-400"
            />
          </div>

          {/* 비밀번호 */}
          <div className="flex flex-col mb-1">
            <label
              htmlFor="password"
              className="text-sm text-gray-700 text-left mb-1"
            />
            <input
              type="password"
              placeholder="password"
              value={password}
              onChange={handlePasswordChange}
              className="caret-lime-500 w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-lime-400"
            />
          </div>

          {/* 비밀번호 확인 */}
          <div className="flex flex-col mb-1">
            <label
              htmlFor="confirm"
              className="text-sm text-gray-700 text-left mb-1"
            />
            <input
              type="password"
              placeholder="confirm password"
              value={confirmPassword}
              onChange={handleConfirmChange}
              className="caret-lime-500 w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-lime-400"
            />
          </div>

          {matchStatus === "match" && (
            <p className="text-sm text-green-600 mt-1">
              비밀번호가 일치합니다.
            </p>
          )}
          {matchStatus === "mismatch" && (
            <p className="text-sm text-red-500 mt-1">
              비밀번호가 일치하지 않습니다.
            </p>
          )}
          {/* 로그인버튼 */}
          <button
            type="submit"
            disabled={!isFormValid}
            className={`w-full mt-6 py-2 rounded-md transition-colors ${
              isFormValid
                ? "bg-lime-500 text-white hover:bg-lime-600"
                : "bg-gray-300 text-gray-500 cursor-not-allowed"
            }`}
          >
            로그인
          </button>

          {/* 회원가입 버튼 */}
          <div className="w-full flex justify-end mb-2">
            <button className="text-xs text-lime-600 underline hover:text-lime-800">
              회원가입
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default LoginPage;
