/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  darkMode: "class",
  theme: {
    extend: {
      fontFamily: {
        paperlogy1: ["Paperlogy-1Thin", "sans-serif"],
        paperlogy2: ["Paperlogy-2ExtraLight", "sans-serif"],
        paperlogy3: ["Paperlogy-3Light", "sans-serif"],
        paperlogy4: ["Paperlogy-4Regular", "sans-serif"],
        paperlogy5: ["Paperlogy-5Medium", "sans-serif"],
        paperlogy6: ["Paperlogy-6SemiBold", "sans-serif"],
        paperlogy7: ["Paperlogy-7Bold", "sans-serif"],
        paperlogy8: ["Paperlogy-8ExtraBold", "sans-serif"],
        paperlogy9: ["Paperlogy-9Black", "sans-serif"],
        consolaNormal: ["Consolas", "sans-serif"],
        consolaBold: ["Consolab", "sans-serif"],
      },
      fontSize: {
        "14px": "0.875rem",
        "16px": "1rem",
        "18px": "1.125rem",
        "20px": "1.25rem",
        "22px": "1.375rem",
        "24px": "1.5rem",
        "28px": "1.75rem",
        "32px": "2rem",
        "34px": "2.125rem",
        "36px": "2.25rem",
      },
    },
  },
  plugins: [],
};
