import { useState, useEffect, useRef } from 'react';
import { MaterialIcon } from './MaterialIcon';

export type ThemeMode = 'system' | 'dark' | 'light';

export interface ThemeSwitcherProps {
  className?: string;
  showLabel?: boolean;
}

export const ThemeSwitcher: React.FC<ThemeSwitcherProps> = ({ className = '', showLabel = false }) => {
  const [theme, setTheme] = useState<ThemeMode>(() => {
    return (localStorage.getItem('dosezy_theme') as ThemeMode) || 'system';
  });
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Apply theme to document element
  useEffect(() => {
    const root = document.documentElement;

    const applyTheme = (mode: ThemeMode) => {
      let isDark = true;
      if (mode === 'system') {
        isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      } else {
        isDark = mode === 'dark';
      }

      if (isDark) {
        root.classList.add('dark');
        root.classList.remove('light');
      } else {
        root.classList.remove('dark');
        root.classList.add('light');
      }
    };

    applyTheme(theme);

    // If system, listen to OS media query changes
    if (theme === 'system') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      const handleChange = () => applyTheme('system');
      mediaQuery.addEventListener('change', handleChange);
      return () => mediaQuery.removeEventListener('change', handleChange);
    }
  }, [theme]);

  // Click outside to close dropdown
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [isOpen]);

  const handleSelectTheme = (mode: ThemeMode) => {
    setTheme(mode);
    localStorage.setItem('dosezy_theme', mode);
    setIsOpen(false);
  };

  const currentIcon = theme === 'dark' ? 'dark_mode' : theme === 'light' ? 'light_mode' : 'contrast';
  const currentLabel = theme === 'dark' ? 'Dark' : theme === 'light' ? 'Light' : 'System';

  const options: { id: ThemeMode; label: string; desc: string; icon: string }[] = [
    { id: 'system', label: 'System', desc: 'Follow OS preference', icon: 'contrast' },
    { id: 'dark', label: 'Dark Mode', desc: 'Dosezy Obsidian Navy', icon: 'dark_mode' },
    { id: 'light', label: 'Light Mode', desc: 'Dosezy Clean White', icon: 'light_mode' },
  ];

  return (
    <div ref={dropdownRef} className={`relative inline-block ${className}`}>
      {/* Trigger Button - Clean icon button by default without text */}
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className={
          showLabel
            ? 'flex items-center space-x-2 px-3 py-2 rounded-2xl bg-white/[0.06] hover:bg-white/[0.1] border border-white/10 text-white text-xs font-semibold backdrop-blur-xl transition-all shadow-md active:scale-95 group focus:outline-none focus:border-sky-500 cursor-pointer'
            : 'w-10 h-10 rounded-2xl bg-white/[0.06] hover:bg-white/[0.1] border border-white/10 text-white flex items-center justify-center backdrop-blur-xl transition-all shadow-md active:scale-95 group focus:outline-none focus:border-sky-500 cursor-pointer'
        }
        title={`Current theme: ${currentLabel}. Click to switch.`}
        aria-label={`Current theme: ${currentLabel}. Click to switch.`}
      >
        <div className="w-7 h-7 rounded-xl bg-sky-500/20 text-sky-400 flex items-center justify-center group-hover:scale-110 transition-transform">
          <MaterialIcon name={currentIcon} className="!text-base" />
        </div>
        {showLabel && (
          <>
            <span className="font-mono text-[11px]">{currentLabel}</span>
            <MaterialIcon
              name="expand_more"
              className={`!text-sm text-neutral-400 transition-transform duration-200 ${isOpen ? 'rotate-180 text-sky-400' : ''}`}
            />
          </>
        )}
      </button>

      {/* Dropdown Menu */}
      {isOpen && (
        <div className="absolute right-0 mt-2 w-52 rounded-2xl bg-[#0a101d] border border-white/15 p-1.5 shadow-2xl z-50 backdrop-blur-2xl animate-in fade-in zoom-in-95 duration-150">
          <div className="px-2.5 py-1.5 border-b border-white/5 mb-1">
            <span className="text-[10px] font-mono uppercase tracking-wider text-neutral-400 block font-bold">
              Interface Theme
            </span>
          </div>

          <div className="space-y-1">
            {options.map((opt) => {
              const isSelected = theme === opt.id;
              return (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() => handleSelectTheme(opt.id)}
                  className={`w-full flex items-center justify-between p-2 rounded-xl text-left transition-all ${
                    isSelected
                      ? 'bg-sky-500/20 text-sky-200 border border-sky-500/30'
                      : 'text-neutral-300 hover:text-white hover:bg-white/5 border border-transparent'
                  }`}
                >
                  <div className="flex items-center space-x-2.5">
                    <div className={`w-7 h-7 rounded-lg flex items-center justify-center ${
                      isSelected ? 'bg-sky-500 text-white' : 'bg-white/5 text-neutral-400'
                    }`}>
                      <MaterialIcon name={opt.icon} className="!text-sm" />
                    </div>
                    <div>
                      <span className="text-xs font-bold block leading-tight">{opt.label}</span>
                      <span className="text-[10px] text-neutral-400 block font-mono">{opt.desc}</span>
                    </div>
                  </div>

                  {isSelected && (
                    <MaterialIcon name="check" className="!text-sm text-sky-400 font-bold shrink-0 ml-1" />
                  )}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
