import { useState, useEffect } from 'react';

const THEME_KEY = 'theme';
const DARK_THEME_VALUE = 'dark';
const LIGHT_THEME_VALUE = 'light'; // 라이트 모드 값도 정의

export default function useDarkMode() {
  const [isDark, setIsDark] = useState(() => {
    // 초기 상태는 localStorage를 직접 읽지 않음
    if (typeof window !== 'undefined' && window.matchMedia) {
      return window.matchMedia("(prefers-color-scheme: dark)").matches;
    }
    return false;
  });

  useEffect(() => {
    let initialThemeIsDark = false;
    if (typeof window !== 'undefined') {
      try {
        const storedTheme = localStorage.getItem(THEME_KEY);
        if (storedTheme === DARK_THEME_VALUE) {
          initialThemeIsDark = true;
        } else if (storedTheme === LIGHT_THEME_VALUE) {
          initialThemeIsDark = false;
        } else { // localStorage에 명시적 설정이 없으면 시스템 설정 따름
          if (window.matchMedia) {
            initialThemeIsDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
          }
        }
      } catch (error) {
        console.warn('[useDarkMode] localStorage에서 테마를 가져오는 데 실패 (PDF 생성 중에는 예상될 수 있음):', error.message);
        // 실패 시 시스템 설정 따름
        if (window.matchMedia) {
          initialThemeIsDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
        }
      }
    }
    setIsDark(initialThemeIsDark);
  }, []);

  // isDark 변경 시 DOM 및 localStorage 업데이트 (안전하게)
  useEffect(() => {
    if (typeof window === 'undefined') return;
    const root = window.document.documentElement;
    if (isDark) {
      root.classList.add(DARK_THEME_VALUE);
      root.classList.remove(LIGHT_THEME_VALUE); // 명시적으로 light 클래스 제거 (필요시)
      try {
        localStorage.setItem(THEME_KEY, DARK_THEME_VALUE);
      } catch (error) {
        console.warn('[useDarkMode] localStorage에 다크 테마 저장 실패 (PDF 생성 중에는 예상될 수 있음):', error.message);
      }
    } else {
      root.classList.remove(DARK_THEME_VALUE);
      root.classList.add(LIGHT_THEME_VALUE); // 명시적으로 light 클래스 추가 (필요시)
      try {
        localStorage.setItem(THEME_KEY, LIGHT_THEME_VALUE);
      } catch (error) {
        console.warn('[useDarkMode] localStorage에 라이트 테마 저장 실패 (PDF 생성 중에는 예상될 수 있음):', error.message);
      }
    }
  }, [isDark]);

  return [isDark, setIsDark] as const;
}