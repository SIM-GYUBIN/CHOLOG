import useDarkMode from "../hooks/useDarkMode";

export default function DarkModeToggle() {
  const [isDark, setIsDark] = useDarkMode();

  return (
    <label className="relative inline-block w-14 h-8 cursor-pointer">
      <input
        type="checkbox"
        checked={isDark}
        onChange={() => setIsDark(!isDark)}
        className="sr-only peer"
      />
      <div className="absolute inset-0 bg-gray-300 dark:bg-gray-100 rounded-full transition-colors peer-checked:bg-zinc-700"></div>
      <div className="absolute left-1 top-1 w-6 h-6 bg-white dark:bg-gray-50 rounded-full flex items-center justify-center text-xs transition-transform duration-300 peer-checked:translate-x-6">
        {isDark ? "🌙" : "☀️"}
      </div>
    </label>
  );
}
