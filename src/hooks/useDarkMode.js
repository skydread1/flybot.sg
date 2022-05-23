import { useEffect, useState } from 'react';

export default function useDarkMode() {
  const [theme, setTheme] = useState(localStorage.theme);
  const nextTheme = theme === 'dark' ? 'light' : 'dark';

  useEffect(() => {
    // Add new theme to html class
    const root = window.document.documentElement;
    root.classList.remove(nextTheme);
    root.classList.add(theme);

    // Save theme in local storage
    localStorage.setItem('theme', theme);

  }, [theme, nextTheme])
  return [nextTheme, setTheme];
}