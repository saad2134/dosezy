"use client";

import React, { useState, useEffect, useRef } from "react";
import {
  Bell,
  Calendar,
  Shield,
  Smartphone,
  Languages,
  AlertTriangle,
  Clock,
  Download,
  Users,
  FileText,
  ChevronDown,
  Menu,
  X,
  PhoneCall,
  Trash2,
  AlertCircle,
  Volume2,
  CheckCircle,
  HelpCircle,
  ExternalLink,
  ChevronRight,
  Sun,
  Moon,
  Laptop,
  Check,
  ChevronLeft,
  Plus,
  Home as HomeIcon,
  Pill,
  ArrowLeft,
  Settings,
  Phone,
  Flame,
  Ambulance,
  ShieldAlert
} from "lucide-react";

export default function Home() {
  const [themeMode, setThemeMode] = useState<"system" | "light" | "dark">("system");
  const [effectiveTheme, setEffectiveTheme] = useState<"light" | "dark">("dark");
  const [themeDropdownOpen, setThemeDropdownOpen] = useState(false);
  const themeDropdownRef = useRef<HTMLDivElement>(null);

  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [selectedLanguage, setSelectedLanguage] = useState("en");
  const [selectedPreviewScreen, setSelectedPreviewScreen] = useState("home");
  const [activeFaq, setActiveFaq] = useState<number | null>(null);
  const [emergencyConfirmOpen, setEmergencyConfirmOpen] = useState(false);
  const [activeProfile, setActiveProfile] = useState("Saad");

  // Selected country in emergency preview
  const [selectedCountry, setSelectedCountry] = useState("IN");

  // Screenshot gallery active index
  const [activeScreenshotIdx, setActiveScreenshotIdx] = useState(0);

  // Theme resolution
  useEffect(() => {
    const savedTheme = localStorage.getItem("dosezy-theme") as "system" | "light" | "dark" | null;
    if (savedTheme) {
      setThemeMode(savedTheme);
    }
  }, []);

  useEffect(() => {
    const applyTheme = () => {
      let resolved: "light" | "dark" = "dark";
      if (themeMode === "system") {
        resolved = window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
      } else {
        resolved = themeMode;
      }
      setEffectiveTheme(resolved);
      if (resolved === "dark") {
        document.documentElement.classList.add("dark");
      } else {
        document.documentElement.classList.remove("dark");
      }
    };

    applyTheme();
    localStorage.setItem("dosezy-theme", themeMode);

    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    const handleChange = () => {
      if (themeMode === "system") {
        applyTheme();
      }
    };
    mediaQuery.addEventListener("change", handleChange);
    return () => mediaQuery.removeEventListener("change", handleChange);
  }, [themeMode]);

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (themeDropdownRef.current && !themeDropdownRef.current.contains(event.target as Node)) {
        setThemeDropdownOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const isDark = effectiveTheme === "dark";

  const screenshots = [
    {
      title: "Home Screen",
      desc: "Daily timeline, scheduled medications, and real-time adherence indicators.",
      src: "https://github.com/user-attachments/assets/116b4a0b-fa72-496c-b56c-305712dd8047"
    },
    {
      title: "Schedule Screen",
      desc: "Interactive calendar with month/day drilldowns and customizable schedules.",
      src: "https://github.com/user-attachments/assets/ae1b400c-594c-4575-a419-0a60b3a24b2e"
    },
    {
      title: "Medicines Inventory",
      desc: "Overview of your prescriptions with dosage amounts, stock levels, and timings.",
      src: "https://github.com/user-attachments/assets/7f772ce5-1412-4781-a9cd-29c40661d6df"
    },
    {
      title: "Add Medicine Form",
      desc: "Simple, large-input fields to add prescription name, dosage, frequency, and picture.",
      src: "https://github.com/user-attachments/assets/3684738e-a7c1-420f-aae3-9ce822a78ac7"
    },
    {
      title: "Menu & Quick Tools",
      desc: "Direct navigation to emergency dialers, multi-profile switches, and data tools.",
      src: "https://github.com/user-attachments/assets/9b7a72d3-5e09-4d12-9b08-feb1584eae17"
    },
    {
      title: "Notification Status",
      desc: "Diagnostic verification of alarm sound levels, battery optimizations, and alert permissions.",
      src: "https://github.com/user-attachments/assets/19baf470-34de-42b6-bc80-9caaaa84e613"
    },
    {
      title: "Profile Management",
      desc: "Manage multiple family accounts and photos without cloud dependency.",
      src: "https://github.com/user-attachments/assets/8d2c10ee-0c32-48a7-b2fd-b606f0db61ae"
    },
    {
      title: "Data Export Feature",
      desc: "Export clean offline PDF medical summaries or JSON backup archives.",
      src: "https://github.com/user-attachments/assets/bdc43e8a-1814-44b2-81e1-55b36fbf09df"
    },
    {
      title: "Preferences & Thresholds",
      desc: "Customize snooze intervals (5-30m) and configure custom Late & Missed thresholds.",
      src: "https://github.com/user-attachments/assets/887d0b85-233e-4e13-8c08-389e2ec73c0c"
    }
  ];

  const translations: Record<string, { label: string; text: string; emergency: string }> = {
    en: {
      label: "English",
      text: "Dosezy simplifies your health and keeps you on track.",
      emergency: "Emergency Contacts"
    },
    es: {
      label: "Español",
      text: "Dosezy simplifica tu salud y te mantiene en el camino.",
      emergency: "Contactos de Emergencia"
    },
    hi: {
      label: "हिन्दी",
      text: "डोज़ी आपके स्वास्थ्य को सरल बनाता है और आपको ट्रैक पर रखता है।",
      emergency: "आपातकालीन संपर्क"
    },
    zh: {
      label: "中文",
      text: "Dosezy 简化您的健康，让您保持正轨。",
      emergency: "紧急联系人"
    },
    pt: {
      label: "Português",
      text: "Dosezy simplifica sua saúde e mantém você no caminho certo.",
      emergency: "Contatos de Emergência"
    },
    ar: {
      label: "العربية",
      text: "Dosezy يبسط صحتك ويبقيك على المسار الصحيح.",
      emergency: "جهات اتصال الطوارئ"
    },
    fr: {
      label: "Français",
      text: "Dosezy simplifie votre santé et vous maintient sur la bonne voie.",
      emergency: "Contacts d'Urgence"
    },
    de: {
      label: "Deutsch",
      text: "Dosezy vereinfacht Ihre Gesundheit und hält Sie auf dem Laufenden.",
      emergency: "Notfallkontakte"
    },
    ja: {
      label: "日本語",
      text: "Dosezyはあなたの健康をシンプルにし、スケジュール通りに進めます。",
      emergency: "緊急連絡先"
    },
    ru: {
      label: "Русский",
      text: "Dosezy упрощает ваше здоровье и помогает держать всё под контролем.",
      emergency: "Экстренные Контакты"
    },
    it: {
      label: "Italiano",
      text: "Dosezy semplifica la tua salute e ti tiene in carreggiata.",
      emergency: "Contatti di Emergenza"
    },
    bn: {
      label: "বাংলা",
      text: "ডোজী আপনার স্বাস্থ্যকে সহজ করে এবং আপনাকে ট্র্যাক এ রাখে।",
      emergency: "জরুরি যোগাযোগ"
    }
  };

  const appFeatures = [
    {
      icon: <Clock className="w-6 h-6 text-[#0277BD] dark:text-[#4FC3F7]" />,
      title: "Configurable Thresholds",
      description: "Define 'Consider Late' (1-3h) and 'Consider Missed' (3-9h) thresholds to match your personal daily rhythm."
    },
    {
      icon: <Languages className="w-6 h-6 text-[#0277BD] dark:text-[#4FC3F7]" />,
      title: "12 Native Languages",
      description: "Handcrafted localization across English, Spanish, Hindi, Chinese, Arabic, French, German, Russian, Japanese, Portuguese, Italian, and Bengali."
    },
    {
      icon: <PhoneCall className="w-6 h-6 text-[#0277BD] dark:text-[#4FC3F7]" />,
      title: "Emergency Services",
      description: "Country-aware automatic emergency dialer featuring 10 major regions with deletion safety confirmation."
    },
    {
      icon: <Bell className="w-6 h-6 text-[#0277BD] dark:text-[#4FC3F7]" />,
      title: "Full-Screen Alarms & Snooze",
      description: "High-contrast persistent alarms with customizable snooze intervals (5, 10, 15, 20, or 30 mins)."
    },
    {
      icon: <Users className="w-6 h-6 text-[#0277BD] dark:text-[#4FC3F7]" />,
      title: "Multi-Profile Care",
      description: "Create and switch between family member profiles to manage distinct prescription regimens."
    },
    {
      icon: <FileText className="w-6 h-6 text-[#0277BD] dark:text-[#4FC3F7]" />,
      title: "PDF & JSON Export",
      description: "Generate clean PDF medical summaries for healthcare providers or JSON archives. 100% offline."
    }
  ];

  const faqs = [
    {
      q: "Does Dosezy share any of my medical data?",
      a: "No. Dosezy is strictly offline-first. All profiles, schedules, alarms, and personal emergency contacts are stored locally on your device in a secure Room database. No telemetry, no third-party tracking, and zero login accounts required."
    },
    {
      q: "How does the emergency contact deletion confirmation work?",
      a: "To prevent accidental deletions during stressful situations, tapping the delete button opens a confirmation popup that must be explicitly verified before a contact is removed."
    },
    {
      q: "What Android versions are supported?",
      a: "Dosezy is built with Jetpack Compose and supports Android 7.0 (Nougat, API Level 24) and all newer Android versions."
    },
    {
      q: "How does theme switching work in the app?",
      a: "The app supports both Light (Ocean Blue #0277BD / #F8F9FA) and Dark (Sky Blue #4FC3F7 / #121212) palettes, respecting your Android system theme by default with zero white-flash transitions."
    }
  ];

  return (
    <div className={`min-h-screen font-sans transition-colors duration-300 ${
      isDark 
        ? "bg-[#121212] text-[#E0E0E0] selection:bg-[#4FC3F7] selection:text-black" 
        : "bg-[#F8F9FA] text-[#1E293B] selection:bg-[#0277BD] selection:text-white"
    }`}>
      
      {/* Ambient Backlight Orbs */}
      <div className={`absolute top-0 left-1/4 w-[500px] h-[500px] rounded-full blur-[140px] pointer-events-none transition-opacity duration-500 ${
        isDark ? "bg-[#0277BD]/15" : "bg-[#4FC3F7]/25"
      }`} />
      <div className={`absolute top-1/3 right-1/4 w-[400px] h-[400px] rounded-full blur-[140px] pointer-events-none transition-opacity duration-500 ${
        isDark ? "bg-[#4FC3F7]/10" : "bg-[#0277BD]/15"
      }`} />

      {/* Main Header */}
      <header className={`sticky top-0 z-50 backdrop-blur-md border-b transition-colors duration-300 ${
        isDark
          ? "bg-[#121212]/85 border-[#2C2C2C]"
          : "bg-white/85 border-[#E2E8F0]"
      }`}>
        <div className="max-w-7xl mx-auto px-6 h-18 flex items-center justify-between">
          
          {/* Brand Logo */}
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-[#0277BD] to-[#4FC3F7] flex items-center justify-center shadow-md shadow-[#0277BD]/20">
              <span className="text-xl font-bold text-white">💊</span>
            </div>
            <div>
              <span className={`text-xl font-extrabold tracking-tight ${
                isDark ? "text-white" : "text-[#0277BD]"
              }`}>
                Dosezy
              </span>
              <span className={`text-[10px] ml-1.5 px-2 py-0.5 rounded-full font-mono font-bold border ${
                isDark
                  ? "bg-[#4FC3F7]/15 text-[#4FC3F7] border-[#4FC3F7]/30"
                  : "bg-[#0277BD]/10 text-[#0277BD] border-[#0277BD]/20"
              }`}>
                v2.1.0
              </span>
            </div>
          </div>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center gap-8 text-sm font-semibold text-zinc-500 dark:text-zinc-400">
            <a href="#features" className="hover:text-[#0277BD] dark:hover:text-[#4FC3F7] transition-colors">Features</a>
            <a href="#screenshots" className="hover:text-[#0277BD] dark:hover:text-[#4FC3F7] transition-colors">Screenshots</a>
            <a href="#preview" className="hover:text-[#0277BD] dark:hover:text-[#4FC3F7] transition-colors">Live Preview</a>
            <a href="#languages" className="hover:text-[#0277BD] dark:hover:text-[#4FC3F7] transition-colors">Translations</a>
            <a href="#faqs" className="hover:text-[#0277BD] dark:hover:text-[#4FC3F7] transition-colors">FAQs</a>
          </nav>

          {/* Top Right Controls: Theme Icon Only Dropdown & Download Button */}
          <div className="hidden md:flex items-center gap-4">
            
            {/* Theme Dropdown (Icon Only) */}
            <div className="relative" ref={themeDropdownRef}>
              <button
                onClick={() => setThemeDropdownOpen(!themeDropdownOpen)}
                className={`p-2.5 rounded-xl border transition-all ${
                  isDark
                    ? "bg-[#1E1E1E] border-[#2C2C2C] text-[#4FC3F7] hover:border-[#4FC3F7]/50"
                    : "bg-white border-[#E2E8F0] text-[#0277BD] hover:border-[#0277BD]/50 shadow-sm"
                }`}
                aria-label="Toggle Theme"
                title={`Theme: ${themeMode}`}
              >
                {themeMode === "system" && <Laptop className="w-4 h-4" />}
                {themeMode === "light" && <Sun className="w-4 h-4 text-amber-500" />}
                {themeMode === "dark" && <Moon className="w-4 h-4 text-[#4FC3F7]" />}
              </button>

              {themeDropdownOpen && (
                <div className={`absolute right-0 mt-2 w-36 rounded-2xl border p-1.5 shadow-xl z-50 backdrop-blur-md animate-in fade-in zoom-in-95 duration-150 ${
                  isDark
                    ? "bg-[#1E1E1E]/95 border-[#2C2C2C] text-[#E0E0E0]"
                    : "bg-white/95 border-[#E2E8F0] text-[#1E293B]"
                }`}>
                  {[
                    { mode: "system", label: "System", icon: <Laptop className="w-4 h-4" /> },
                    { mode: "light", label: "Light", icon: <Sun className="w-4 h-4" /> },
                    { mode: "dark", label: "Dark", icon: <Moon className="w-4 h-4" /> }
                  ].map((t) => (
                    <button
                      key={t.mode}
                      onClick={() => {
                        setThemeMode(t.mode as "system" | "light" | "dark");
                        setThemeDropdownOpen(false);
                      }}
                      className={`flex items-center justify-between w-full px-3 py-2 rounded-xl text-xs font-semibold transition-all ${
                        themeMode === t.mode
                          ? isDark
                            ? "bg-[#0277BD] text-white"
                            : "bg-[#0277BD]/10 text-[#0277BD]"
                          : "hover:bg-zinc-500/10"
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        {t.icon}
                        <span>{t.label}</span>
                      </div>
                      {themeMode === t.mode && <Check className="w-3.5 h-3.5" />}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Download Action */}
            <a
              href="https://github.com/saad2134/dosezy/releases"
              target="_blank"
              rel="noreferrer"
              className={`flex items-center gap-2 px-5 py-2 rounded-full text-xs font-bold transition-all shadow-md ${
                isDark
                  ? "bg-gradient-to-r from-[#0277BD] to-[#4FC3F7] text-white shadow-[#0277BD]/20 hover:opacity-95"
                  : "bg-[#0277BD] text-white hover:bg-[#01579B]"
              }`}
            >
              <Download className="w-4 h-4" />
              Download APK
            </a>
          </div>

          {/* Mobile Theme Toggle Button & Hamburger */}
          <div className="flex md:hidden items-center gap-2">
            <button
              onClick={() => {
                const next = themeMode === "system" ? "dark" : themeMode === "dark" ? "light" : "system";
                setThemeMode(next);
              }}
              className={`p-2.5 rounded-xl border text-xs font-bold ${
                isDark ? "border-[#2C2C2C] bg-[#1E1E1E]" : "border-[#E2E8F0] bg-white shadow-sm"
              }`}
              title={`Theme: ${themeMode}`}
            >
              {themeMode === "system" && <Laptop className="w-4 h-4 text-[#4FC3F7]" />}
              {themeMode === "light" && <Sun className="w-4 h-4 text-amber-500" />}
              {themeMode === "dark" && <Moon className="w-4 h-4 text-[#4FC3F7]" />}
            </button>
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 text-zinc-500 dark:text-zinc-400 hover:text-white"
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>

        {/* Mobile Navigation Drawer */}
        {mobileMenuOpen && (
          <div className={`md:hidden border-b px-6 py-6 flex flex-col gap-5 ${
            isDark ? "bg-[#121212] border-[#2C2C2C]" : "bg-white border-[#E2E8F0]"
          }`}>
            <nav className="flex flex-col gap-3 font-semibold text-sm">
              <a href="#features" onClick={() => setMobileMenuOpen(false)}>Features</a>
              <a href="#screenshots" onClick={() => setMobileMenuOpen(false)}>Screenshots</a>
              <a href="#preview" onClick={() => setMobileMenuOpen(false)}>Live Preview</a>
              <a href="#languages" onClick={() => setMobileMenuOpen(false)}>Translations</a>
              <a href="#faqs" onClick={() => setMobileMenuOpen(false)}>FAQs</a>
            </nav>
            <a
              href="https://github.com/saad2134/dosezy/releases"
              target="_blank"
              rel="noreferrer"
              className="flex items-center justify-center gap-2 w-full py-3 rounded-xl bg-gradient-to-r from-[#0277BD] to-[#4FC3F7] text-white font-bold shadow-md text-sm"
            >
              <Download className="w-4 h-4" />
              Download Latest APK (v2.1.0)
            </a>
          </div>
        )}
      </header>

      {/* Hero Section */}
      <section className="max-w-7xl mx-auto px-6 pt-12 pb-20 md:py-28 grid md:grid-cols-12 gap-16 items-center">
        <div className="md:col-span-7 flex flex-col items-start gap-8">
          
          <div className={`inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full text-xs font-bold tracking-wide border ${
            isDark
              ? "bg-[#0277BD]/20 border-[#4FC3F7]/30 text-[#4FC3F7]"
              : "bg-[#0277BD]/10 border-[#0277BD]/20 text-[#0277BD]"
          }`}>
            <span className="w-2 h-2 rounded-full bg-[#4FC3F7] animate-pulse" />
            V2.1.0 Released: Accessibility & Safety Enhancements
          </div>

          <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight leading-tight">
            Medicine Adherence, <br />
            <span className="bg-gradient-to-r from-[#0277BD] via-[#29B6F6] to-[#4FC3F7] bg-clip-text text-transparent">
              Simplified.
            </span>
          </h1>

          <p className="text-lg leading-relaxed max-w-xl text-zinc-600 dark:text-zinc-400">
            Dosezy is an open-source Android medication tracker crafted with accessibility-first principles: clear large typography, high-contrast layouts, custom snooze reminders, and 12 native global languages.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 w-full sm:w-auto">
            <a
              href="https://github.com/saad2134/dosezy/releases"
              target="_blank"
              rel="noreferrer"
              className="flex items-center justify-center gap-3 px-8 py-4 rounded-full bg-gradient-to-r from-[#0277BD] to-[#4FC3F7] text-white font-bold tracking-wide shadow-xl shadow-[#0277BD]/20 hover:scale-[1.02] transition-all group"
            >
              <Download className="w-5 h-5" />
              Download APK (v2.1.0)
              <ChevronRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </a>
            <a
              href="https://github.com/saad2134/dosezy"
              target="_blank"
              rel="noreferrer"
              className={`flex items-center justify-center gap-3 px-8 py-4 rounded-full font-bold border transition-all ${
                isDark
                  ? "bg-[#1E1E1E] border-[#2C2C2C] text-[#E0E0E0] hover:border-[#4FC3F7]/40"
                  : "bg-white border-[#E2E8F0] text-[#1E293B] hover:border-[#0277BD]/40 shadow-sm"
              }`}
            >
              View GitHub Repo
              <ExternalLink className="w-4 h-4 opacity-60" />
            </a>
          </div>

          <div className="flex items-center gap-6 text-xs font-mono text-zinc-500 dark:text-zinc-400">
            <div>✓ ZERO ADS</div>
            <div>✓ 100% OFFLINE ROOM DB</div>
            <div>✓ JETPACK COMPOSE</div>
          </div>
        </div>

        {/* Hero Interactive Phone Mockup (Exact Jetpack Compose TopBar & NavigationBar reproduction) */}
        <div className="md:col-span-5 flex justify-center relative">
          <div className={`relative w-80 h-[640px] rounded-[48px] border-[6px] shadow-2xl overflow-hidden flex flex-col transition-colors duration-300 ${
            isDark ? "border-[#2C2C2C] bg-[#121212]" : "border-[#CBD5E1] bg-[#F8F9FA]"
          }`}>
            
            {/* Dynamic Camera Notch (Adaptive Light/Dark Theme) */}
            <div className={`absolute top-2 left-1/2 -translate-x-1/2 w-28 h-5 rounded-full z-20 flex items-center justify-center border transition-colors duration-300 ${
              isDark ? "bg-[#252525] border-[#333333]" : "bg-[#E2E8F0] border-[#CBD5E1]"
            }`}>
              <div className={`w-2.5 h-2.5 rounded-full ${isDark ? "bg-[#161616]" : "bg-[#94A3B8]"}`} />
            </div>

            {/* Android Status Bar */}
            <div className={`h-11 border-b pt-4 px-6 flex items-center justify-between text-[11px] font-mono ${
              isDark ? "bg-[#1E1E1E] border-[#2C2C2C] text-zinc-400" : "bg-white border-[#E2E8F0] text-zinc-600"
            }`}>
              <span>09:41</span>
              <div className="flex items-center gap-2">
                <span>5G</span>
                <span>🔋 100%</span>
              </div>
            </div>

            {/* Jetpack Compose TopBar Reproduction */}
            <div className={`px-4 py-3 border-b flex items-center justify-between transition-colors duration-300 ${
              isDark ? "bg-[#1E1E1E] border-[#2C2C2C]" : "bg-white border-[#E2E8F0]"
            }`}>
              {/* Profile Avatar & Greeting */}
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setActiveProfile(activeProfile === "Saad" ? "Mom" : "Saad")}
                  className={`w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm border-2 transition-all ${
                    isDark
                      ? "bg-[#0277BD] text-white border-[#4FC3F7]"
                      : "bg-[#0277BD] text-white border-[#0277BD]/30"
                  }`}
                  title="Click to switch profile"
                >
                  {activeProfile === "Saad" ? "S" : "M"}
                </button>
                <div className="text-left">
                  <div className="flex items-center gap-1">
                    <span className={`text-xs font-extrabold ${isDark ? "text-white" : "text-[#1E293B]"}`}>
                      Good Morning, {activeProfile}
                    </span>
                  </div>
                  <p className="text-[10px] text-zinc-500 dark:text-zinc-400 font-medium">
                    Saturday, August 8
                  </p>
                </div>
              </div>

              {/* Notification Status Bell Diagnostic Icon */}
              <div className={`w-9 h-9 rounded-full flex items-center justify-center relative border transition-all ${
                isDark ? "bg-[#2C2C2C] border-[#333333] text-zinc-300" : "bg-[#F1F5F9] border-[#E2E8F0] text-zinc-700"
              }`}>
                <Bell className="w-4 h-4" />
                <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-emerald-500" />
              </div>
            </div>

            {/* Mock Screen Content */}
            <div className={`flex-1 p-4 overflow-y-auto flex flex-col gap-3 font-sans text-left transition-colors duration-300 ${
              isDark ? "bg-[#121212]" : "bg-[#F8F9FA]"
            }`}>
              
              {/* Daily Progress Card */}
              <div className={`flex items-center justify-between border rounded-2xl p-3.5 shadow-sm ${
                isDark ? "bg-[#1E1E1E] border-[#2C2C2C]" : "bg-white border-[#E2E8F0]"
              }`}>
                <div>
                  <span className="text-[10px] uppercase tracking-wider text-zinc-500 dark:text-zinc-400 font-bold">Today's Medicines</span>
                  <h3 className={`text-base font-extrabold mt-0.5 ${isDark ? "text-white" : "text-[#1E293B]"}`}>
                    {activeProfile === "Saad" ? "1 of 2 Taken" : "All Taken"}
                  </h3>
                </div>
                <div className={`w-10 h-10 rounded-full border-4 flex items-center justify-center text-xs font-bold ${
                  isDark ? "border-[#2C2C2C] border-t-[#4FC3F7] text-[#4FC3F7]" : "border-[#E2E8F0] border-t-[#0277BD] text-[#0277BD]"
                }`}>
                  {activeProfile === "Saad" ? "50%" : "100%"}
                </div>
              </div>

              {/* Medication Card 1: Metformin (Taken) */}
              <div className={`border rounded-2xl p-3.5 flex flex-col gap-2.5 relative overflow-hidden shadow-sm ${
                isDark ? "bg-[#1E1E1E] border-[#2C2C2C]" : "bg-white border-[#E2E8F0]"
              }`}>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-lg ${
                      isDark ? "bg-[#2C2C2C]" : "bg-[#F1F5F9]"
                    }`}>
                      💊
                    </div>
                    <div>
                      <h4 className={`text-sm font-bold leading-tight ${isDark ? "text-white" : "text-[#1E293B]"}`}>
                        Metformin
                      </h4>
                      <p className="text-[11px] text-zinc-500 dark:text-zinc-400">500mg • 1 Tablet (After Food)</p>
                    </div>
                  </div>
                </div>

                <div className={`h-px ${isDark ? "bg-[#2C2C2C]" : "bg-[#F1F5F9]"}`} />

                <div className="flex justify-between items-center text-xs">
                  <span className={isDark ? "text-zinc-400" : "text-zinc-500"}>Time: <strong className={isDark ? "text-white" : "text-[#1E293B]"}>08:00 AM</strong></span>
                  <span className={`text-[11px] font-bold px-2.5 py-0.5 rounded-full flex items-center gap-1 ${
                    isDark ? "bg-[#03DAC5]/15 text-[#03DAC5] border border-[#03DAC5]/30" : "bg-emerald-50 text-emerald-700 border border-emerald-200"
                  }`}>
                    <CheckCircle className="w-3 h-3" /> Taken (08:05 AM)
                  </span>
                </div>
              </div>

              {/* Medication Card 2: Multivitamin (Late) */}
              {activeProfile === "Saad" && (
                <div className={`border rounded-2xl p-3.5 flex flex-col gap-2.5 relative overflow-hidden shadow-sm ${
                  isDark ? "bg-[#1E1E1E] border-[#2C2C2C]" : "bg-white border-[#E2E8F0]"
                }`}>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-lg ${
                        isDark ? "bg-[#2C2C2C]" : "bg-[#F1F5F9]"
                      }`}>
                        🥛
                      </div>
                      <div>
                        <h4 className={`text-sm font-bold leading-tight ${isDark ? "text-white" : "text-[#1E293B]"}`}>
                          Multivitamin
                        </h4>
                        <p className="text-[11px] text-zinc-500 dark:text-zinc-400">1 Capsule (Morning)</p>
                      </div>
                    </div>
                  </div>

                  <div className={`h-px ${isDark ? "bg-[#2C2C2C]" : "bg-[#F1F5F9]"}`} />

                  <div className="flex justify-between items-center text-xs">
                    <span className={isDark ? "text-zinc-400" : "text-zinc-500"}>Time: <strong className={isDark ? "text-white" : "text-[#1E293B]"}>08:00 AM</strong></span>
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                      isDark ? "bg-amber-500/15 text-amber-400 border border-amber-500/30" : "bg-amber-100 text-amber-800 border border-amber-200"
                    }`}>
                      Late (1h 41m ago)
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 mt-1">
                    <button className="py-2 bg-[#0277BD] hover:bg-[#01579B] text-white text-xs font-bold rounded-xl transition-all shadow-sm">
                      Take Late
                    </button>
                    <button className={`py-2 text-xs font-bold rounded-xl transition-all ${
                      isDark ? "bg-[#2C2C2C] hover:bg-[#383838] text-zinc-300" : "bg-[#F1F5F9] hover:bg-zinc-200 text-zinc-700"
                    }`}>
                      Snooze 10m
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* Jetpack Compose 5-Tab NavigationBar Reproduction */}
            <div className={`h-16 border-t px-2 flex justify-around items-center transition-colors duration-300 ${
              isDark ? "bg-[#1E1E1E] border-[#2C2C2C]" : "bg-white border-[#E2E8F0]"
            }`}>
              {/* Home */}
              <div className={`flex flex-col items-center gap-0.5 ${isDark ? "text-[#4FC3F7]" : "text-[#0277BD]"}`}>
                <HomeIcon className="w-5 h-5" />
                <span className="text-[9px] font-bold">Home</span>
              </div>
              {/* Schedule */}
              <div className="flex flex-col items-center gap-0.5 text-zinc-400 hover:text-zinc-600 dark:hover:text-white cursor-pointer">
                <Calendar className="w-5 h-5" />
                <span className="text-[9px] font-medium">Schedule</span>
              </div>
              {/* Add (Elevated Center Plus Button) */}
              <div className="flex flex-col items-center -mt-4 cursor-pointer">
                <div className="w-10 h-10 rounded-full bg-[#0277BD] text-white flex items-center justify-center shadow-lg shadow-[#0277BD]/30 hover:scale-105 transition-transform">
                  <Plus className="w-5 h-5" />
                </div>
                <span className="text-[9px] font-medium mt-0.5 text-zinc-400">Add</span>
              </div>
              {/* Medicines */}
              <div className="flex flex-col items-center gap-0.5 text-zinc-400 hover:text-zinc-600 dark:hover:text-white cursor-pointer">
                <Pill className="w-5 h-5" />
                <span className="text-[9px] font-medium">Medicines</span>
              </div>
              {/* Menu */}
              <div className="flex flex-col items-center gap-0.5 text-zinc-400 hover:text-zinc-600 dark:hover:text-white cursor-pointer">
                <Menu className="w-5 h-5" />
                <span className="text-[9px] font-medium">Menu</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Official App Screenshots Showcase */}
      <section id="screenshots" className={`py-24 border-t transition-colors duration-300 ${
        isDark ? "bg-[#161616] border-[#2C2C2C]" : "bg-[#F1F5F9] border-[#E2E8F0]"
      }`}>
        <div className="max-w-7xl mx-auto px-6">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <h2 className="text-3xl sm:text-5xl font-extrabold tracking-tight">
              App Screenshots
            </h2>
            <p className="mt-4 text-zinc-500 dark:text-zinc-400 leading-relaxed">
              Explore the clean, intuitive interface designed for clear readability and seamless medication tracking.
            </p>
          </div>

          {/* Screenshot Carousel / Showcase */}
          <div className="grid lg:grid-cols-12 gap-12 items-center">
            
            {/* Screenshot list selector */}
            <div className="lg:col-span-5 flex flex-col gap-3">
              {screenshots.map((s, idx) => (
                <button
                  key={idx}
                  onClick={() => setActiveScreenshotIdx(idx)}
                  className={`text-left p-4 rounded-2xl border transition-all ${
                    activeScreenshotIdx === idx
                      ? isDark
                        ? "bg-[#0277BD]/20 border-[#4FC3F7] text-white"
                        : "bg-white border-[#0277BD] text-[#0277BD] shadow-md"
                      : isDark
                        ? "bg-[#1E1E1E] border-[#2C2C2C] text-zinc-400 hover:border-[#444444]"
                        : "bg-white/60 border-[#E2E8F0] text-zinc-600 hover:border-zinc-300"
                  }`}
                >
                  <h4 className="text-sm font-bold">{s.title}</h4>
                  <p className="text-xs opacity-80 mt-0.5">{s.desc}</p>
                </button>
              ))}
            </div>

            {/* Active Screenshot Display Frame */}
            <div className="lg:col-span-7 flex flex-col items-center justify-center">
              <div className={`relative p-4 rounded-[40px] border-[6px] shadow-2xl max-w-xs sm:max-w-sm ${
                isDark ? "border-[#2C2C2C] bg-black" : "border-[#CBD5E1] bg-white"
              }`}>
                <img
                  src={screenshots[activeScreenshotIdx].src}
                  alt={screenshots[activeScreenshotIdx].title}
                  className="rounded-[28px] w-full h-auto object-cover"
                />
              </div>
              <div className="flex items-center gap-4 mt-6">
                <button
                  onClick={() => setActiveScreenshotIdx((prev) => (prev > 0 ? prev - 1 : screenshots.length - 1))}
                  className={`p-3 rounded-full border transition-all ${
                    isDark ? "border-zinc-700 bg-zinc-800 text-white hover:bg-zinc-700" : "border-zinc-300 bg-white text-zinc-800 hover:bg-zinc-100 shadow-sm"
                  }`}
                >
                  <ChevronLeft className="w-5 h-5" />
                </button>
                <span className="text-xs font-mono font-bold">
                  {activeScreenshotIdx + 1} / {screenshots.length}
                </span>
                <button
                  onClick={() => setActiveScreenshotIdx((prev) => (prev < screenshots.length - 1 ? prev + 1 : 0))}
                  className={`p-3 rounded-full border transition-all ${
                    isDark ? "border-zinc-700 bg-zinc-800 text-white hover:bg-zinc-700" : "border-zinc-300 bg-white text-zinc-800 hover:bg-zinc-100 shadow-sm"
                  }`}
                >
                  <ChevronRight className="w-5 h-5" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section id="features" className={`max-w-7xl mx-auto px-6 py-24 border-t ${
        isDark ? "border-[#2C2C2C]" : "border-[#E2E8F0]"
      }`}>
        <div className="text-center max-w-2xl mx-auto mb-20">
          <h2 className="text-3xl sm:text-5xl font-extrabold tracking-tight">
            Features Tailored For Accessibility
          </h2>
          <p className="mt-4 text-zinc-500 dark:text-zinc-400 leading-relaxed">
            Dosezy combines simplicity with powerful offline medication schedule tools.
          </p>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-8">
          {appFeatures.map((feat, idx) => (
            <div
              key={idx}
              className={`rounded-3xl p-8 border transition-all hover:scale-[1.01] ${
                isDark
                  ? "bg-[#1E1E1E] border-[#2C2C2C] hover:border-[#4FC3F7]/40 shadow-lg shadow-black/20"
                  : "bg-white border-[#E2E8F0] hover:border-[#0277BD]/30 shadow-md"
              }`}
            >
              <div className={`w-12 h-12 rounded-2xl flex items-center justify-center mb-6 border ${
                isDark ? "bg-[#2C2C2C] border-[#333333]" : "bg-[#0277BD]/10 border-[#0277BD]/20"
              }`}>
                {feat.icon}
              </div>
              <h3 className="text-xl font-bold mb-2">{feat.title}</h3>
              <p className="text-sm leading-relaxed text-zinc-500 dark:text-zinc-400">{feat.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Interactive App Previews Section (Exact Jetpack Compose Screen Reproduction) */}
      <section id="preview" className={`max-w-7xl mx-auto px-6 py-24 border-t ${
        isDark ? "border-[#2C2C2C]" : "border-[#E2E8F0]"
      }`}>
        <div className="grid md:grid-cols-12 gap-16 items-center">
          <div className="md:col-span-5 flex flex-col items-start gap-6">
            <h2 className="text-3xl sm:text-5xl font-extrabold tracking-tight">
              Interactive Previews
            </h2>
            <p className="text-zinc-500 dark:text-zinc-400 leading-relaxed">
              Experience the actual screen workflows and responsive UI elements built with Jetpack Compose.
            </p>

            <div className="flex flex-col gap-3 w-full">
              {[
                { id: "home", title: "Home View & Timeline", desc: "View scheduled medications, daily status badges, and late alerts." },
                { id: "alarm", title: "Full-Screen Alarm Overlay", desc: "High-contrast reminder screen with quick snooze and taken buttons." },
                { id: "emergency", title: "Emergency & Safety Deletion", desc: "Country-aware emergency dialing with confirmation-secured contact removal." }
              ].map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => {
                    setSelectedPreviewScreen(tab.id);
                    if (tab.id !== "emergency") setEmergencyConfirmOpen(false);
                  }}
                  className={`w-full text-left p-5 rounded-2xl border transition-all ${
                    selectedPreviewScreen === tab.id
                      ? isDark
                        ? "bg-[#0277BD]/20 border-[#4FC3F7] text-white"
                        : "bg-white border-[#0277BD] text-[#0277BD] shadow-md"
                      : isDark
                        ? "bg-[#1E1E1E] border-[#2C2C2C] text-zinc-400"
                        : "bg-white/60 border-[#E2E8F0] text-zinc-600"
                  }`}
                >
                  <h4 className="font-bold text-sm">{tab.title}</h4>
                  <p className="text-xs opacity-75 mt-1">{tab.desc}</p>
                </button>
              ))}
            </div>
          </div>

          <div className="md:col-span-7 flex justify-center relative">
            <div className={`relative w-85 h-[640px] rounded-[48px] border-[6px] shadow-2xl overflow-hidden flex flex-col transition-colors duration-300 ${
              isDark ? "border-[#2C2C2C] bg-[#121212]" : "border-[#CBD5E1] bg-[#F8F9FA]"
            }`}>
              
              {/* Dynamic Camera Notch (Adaptive Light/Dark Theme) */}
              <div className={`absolute top-2 left-1/2 -translate-x-1/2 w-28 h-5 rounded-full z-20 flex items-center justify-center border transition-colors duration-300 ${
                isDark ? "bg-[#252525] border-[#333333]" : "bg-[#E2E8F0] border-[#CBD5E1]"
              }`}>
                <div className={`w-2.5 h-2.5 rounded-full ${isDark ? "bg-[#161616]" : "bg-[#94A3B8]"}`} />
              </div>

              {/* Status Bar */}
              <div className={`h-11 border-b pt-4 px-6 flex items-center justify-between text-[11px] font-mono ${
                isDark ? "bg-[#1E1E1E] border-[#2C2C2C] text-zinc-400" : "bg-white border-[#E2E8F0] text-zinc-600"
              }`}>
                <span>09:41</span>
                <div className="flex items-center gap-2">
                  <span>5G</span>
                  <span>🔋 100%</span>
                </div>
              </div>

              {/* TopBar for Current Screen */}
              <div className={`px-4 py-3 border-b flex items-center justify-between transition-colors duration-300 ${
                isDark ? "bg-[#1E1E1E] border-[#2C2C2C]" : "bg-white border-[#E2E8F0]"
              }`}>
                {selectedPreviewScreen === "emergency" ? (
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setSelectedPreviewScreen("home")}
                      className={`p-1.5 rounded-lg ${isDark ? "text-zinc-400 hover:text-white" : "text-zinc-600 hover:text-black"}`}
                    >
                      <ArrowLeft className="w-5 h-5" />
                    </button>
                    <h3 className="text-sm font-extrabold text-red-500">Emergency Services</h3>
                  </div>
                ) : (
                  <div className="flex items-center gap-3">
                    <div className={`w-9 h-9 rounded-full flex items-center justify-center font-bold text-xs ${
                      isDark ? "bg-[#0277BD] text-white" : "bg-[#0277BD] text-white"
                    }`}>
                      S
                    </div>
                    <div className="text-left">
                      <span className={`text-xs font-extrabold ${isDark ? "text-white" : "text-[#1E293B]"}`}>
                        {selectedPreviewScreen === "alarm" ? "Active Reminder" : "Today's Schedule"}
                      </span>
                      <p className="text-[9px] text-zinc-500">Aug 8, 2026</p>
                    </div>
                  </div>
                )}

                <div className={`w-8 h-8 rounded-full flex items-center justify-center border text-xs ${
                  isDark ? "bg-[#2C2C2C] border-[#333333] text-zinc-300" : "bg-[#F1F5F9] border-[#E2E8F0] text-zinc-700"
                }`}>
                  <Bell className="w-3.5 h-3.5" />
                </div>
              </div>

              {/* Screen Body */}
              <div className={`flex-1 p-4 overflow-y-auto flex flex-col gap-3 font-sans transition-colors duration-300 ${
                isDark ? "bg-[#121212]" : "bg-[#F8F9FA]"
              }`}>
                
                {/* 1. Home View Screen */}
                {selectedPreviewScreen === "home" && (
                  <div className="flex flex-col gap-3 animate-in fade-in duration-300">
                    <div className={`border rounded-2xl p-3.5 shadow-sm ${
                      isDark ? "bg-[#1E1E1E] border-[#2C2C2C]" : "bg-white border-[#E2E8F0]"
                    }`}>
                      <div className="flex items-center justify-between">
                        <div>
                          <span className="text-[10px] font-bold uppercase tracking-wider text-zinc-400">Adherence Summary</span>
                          <h4 className={`text-base font-extrabold ${isDark ? "text-white" : "text-[#1E293B]"}`}>1 of 2 Taken</h4>
                        </div>
                        <span className={`text-xs font-extrabold px-2.5 py-1 rounded-full ${
                          isDark ? "bg-[#0277BD]/20 text-[#4FC3F7]" : "bg-[#0277BD]/10 text-[#0277BD]"
                        }`}>
                          50%
                        </span>
                      </div>
                    </div>

                    <div className={`border rounded-2xl p-3.5 flex flex-col gap-2 shadow-sm ${
                      isDark ? "bg-[#1E1E1E] border-[#2C2C2C]" : "bg-white border-[#E2E8F0]"
                    }`}>
                      <div className="flex justify-between items-start">
                        <div className="flex gap-2.5">
                          <div className={`w-9 h-9 rounded-xl flex items-center justify-center text-base ${
                            isDark ? "bg-[#2C2C2C]" : "bg-[#F1F5F9]"
                          }`}>
                            💊
                          </div>
                          <div>
                            <h4 className={`text-xs font-bold ${isDark ? "text-white" : "text-[#1E293B]"}`}>Atorvastatin</h4>
                            <p className="text-[10px] text-zinc-400">10mg • Night</p>
                          </div>
                        </div>
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                          isDark ? "bg-amber-500/15 text-amber-400 border border-amber-500/30" : "bg-amber-100 text-amber-800 border border-amber-200"
                        }`}>
                          Late (2h ago)
                        </span>
                      </div>
                      <div className="flex gap-2 mt-1">
                        <button className="flex-1 py-1.5 bg-[#0277BD] text-white text-[11px] font-bold rounded-lg">
                          Take Late
                        </button>
                        <button className={`flex-1 py-1.5 text-[11px] font-semibold rounded-lg ${
                          isDark ? "bg-[#2C2C2C] text-zinc-400" : "bg-[#F1F5F9] text-zinc-600"
                        }`}>
                          Ignore
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {/* 2. Full-Screen Alarm Overlay */}
                {selectedPreviewScreen === "alarm" && (
                  <div className="flex flex-col flex-1 items-center justify-center text-center gap-5 animate-in fade-in duration-300 py-4">
                    <div className={`w-18 h-18 rounded-full border-2 flex items-center justify-center text-3xl animate-bounce ${
                      isDark ? "bg-[#0277BD]/20 border-[#4FC3F7]" : "bg-[#0277BD]/10 border-[#0277BD]"
                    }`}>
                      🔔
                    </div>
                    <div>
                      <span className={`text-[10px] font-extrabold tracking-wider uppercase ${isDark ? "text-[#4FC3F7]" : "text-[#0277BD]"}`}>
                        Medication Alarm
                      </span>
                      <h3 className={`text-xl font-extrabold mt-1 ${isDark ? "text-white" : "text-[#1E293B]"}`}>
                        Metformin (500mg)
                      </h3>
                      <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">Time: 08:00 AM • 1 Tablet After Food</p>
                    </div>

                    <div className="flex flex-col gap-2 w-full mt-2">
                      <button className="w-full py-2.5 rounded-xl bg-[#0277BD] text-white font-bold text-xs shadow-md">
                        ✓ Mark as Taken
                      </button>
                      <div className="grid grid-cols-2 gap-2">
                        <button className={`py-2 rounded-xl border text-[11px] font-semibold ${
                          isDark ? "bg-[#1E1E1E] border-[#2C2C2C] text-zinc-300" : "bg-white border-[#E2E8F0] text-zinc-700"
                        }`}>
                          Snooze (10m)
                        </button>
                        <button className={`py-2 rounded-xl border text-[11px] font-semibold ${
                          isDark ? "bg-[#1E1E1E] border-[#2C2C2C] text-zinc-300" : "bg-white border-[#E2E8F0] text-zinc-700"
                        }`}>
                          Mark Late
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {/* 3. Emergency Screen with Real Country List & Tintless Safety Popup */}
                {selectedPreviewScreen === "emergency" && (
                  <div className="flex flex-col gap-3 animate-in fade-in duration-300 relative h-full">
                    
                    {/* Country Selector Dropdown representation */}
                    <div className={`p-2.5 rounded-xl border flex items-center justify-between text-xs font-semibold ${
                      isDark ? "bg-[#1E1E1E] border-[#2C2C2C] text-white" : "bg-white border-[#E2E8F0] text-[#1E293B]"
                    }`}>
                      <div className="flex items-center gap-2">
                        <span>🇮🇳</span>
                        <span>India (National 112)</span>
                      </div>
                      <ChevronDown className="w-3.5 h-3.5 opacity-60" />
                    </div>

                    {/* Personal Emergency Contact Card */}
                    <div className={`border rounded-2xl p-3 shadow-sm ${
                      isDark ? "bg-[#1E1E1E] border-[#2C2C2C]" : "bg-white border-[#E2E8F0]"
                    }`}>
                      <div className="flex justify-between items-center">
                        <div className="flex items-center gap-2.5">
                          <div className={`w-8 h-8 rounded-full flex items-center justify-center ${
                            isDark ? "bg-[#2C2C2C] text-[#4FC3F7]" : "bg-[#F1F5F9] text-[#0277BD]"
                          }`}>
                            <Users className="w-4 h-4" />
                          </div>
                          <div>
                            <h4 className={`text-xs font-bold ${isDark ? "text-white" : "text-[#1E293B]"}`}>
                              Dr. Robert Chen
                            </h4>
                            <p className="text-[10px] text-zinc-400">+91 98765 43210 • Cardiologist</p>
                          </div>
                        </div>
                        <button
                          onClick={() => setEmergencyConfirmOpen(true)}
                          className={`p-1.5 rounded-lg transition-all ${
                            isDark ? "text-zinc-400 hover:text-red-400 hover:bg-[#2C2C2C]" : "text-zinc-500 hover:text-red-600 hover:bg-red-50"
                          }`}
                          title="Delete contact"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>

                    {/* Emergency Delete Safety Confirmation Modal (Tintless Compose Dialog) */}
                    {emergencyConfirmOpen && (
                      <div className="absolute inset-0 bg-black/85 backdrop-blur-sm z-30 flex items-center justify-center p-3">
                        <div className={`border rounded-3xl p-5 text-center max-w-xs flex flex-col items-center gap-3 shadow-2xl ${
                          isDark ? "bg-[#1E1E1E] border-[#2C2C2C]" : "bg-white border-[#E2E8F0]"
                        }`}>
                          <div className="w-10 h-10 rounded-full bg-red-500/15 flex items-center justify-center text-red-500">
                            <AlertCircle className="w-5 h-5" />
                          </div>
                          <div>
                            <h4 className={`text-sm font-extrabold ${isDark ? "text-white" : "text-[#1E293B]"}`}>
                              Remove Contact?
                            </h4>
                            <p className="text-[11px] text-zinc-400 mt-1 leading-relaxed">
                              Are you sure you want to delete this personal emergency contact?
                            </p>
                          </div>
                          <div className="grid grid-cols-2 gap-2 w-full mt-1">
                            <button
                              onClick={() => setEmergencyConfirmOpen(false)}
                              className="py-2 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-xl transition-all"
                            >
                              Delete
                            </button>
                            <button
                              onClick={() => setEmergencyConfirmOpen(false)}
                              className={`py-2 text-xs rounded-xl transition-all font-semibold ${
                                isDark ? "bg-[#2C2C2C] text-zinc-300" : "bg-[#F1F5F9] text-zinc-700"
                              }`}
                            >
                              Cancel
                            </button>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* National Dialers List */}
                    <div className={`border rounded-2xl p-3 flex flex-col gap-2 shadow-sm ${
                      isDark ? "bg-[#1E1E1E] border-[#2C2C2C]" : "bg-white border-[#E2E8F0]"
                    }`}>
                      <span className="text-[9px] font-bold uppercase tracking-wider text-zinc-400">Emergency Services</span>
                      <div className="flex justify-between items-center py-0.5 text-xs">
                        <span className="flex items-center gap-2"><Ambulance className="w-3.5 h-3.5 text-red-400" /> Ambulance</span>
                        <span className={`font-bold ${isDark ? "text-[#4FC3F7]" : "text-[#0277BD]"}`}>108</span>
                      </div>
                      <div className="flex justify-between items-center py-0.5 text-xs">
                        <span className="flex items-center gap-2"><ShieldAlert className="w-3.5 h-3.5 text-blue-400" /> Police</span>
                        <span className={`font-bold ${isDark ? "text-[#4FC3F7]" : "text-[#0277BD]"}`}>100</span>
                      </div>
                      <div className="flex justify-between items-center py-0.5 text-xs">
                        <span className="flex items-center gap-2"><Flame className="w-3.5 h-3.5 text-amber-400" /> Fire</span>
                        <span className={`font-bold ${isDark ? "text-[#4FC3F7]" : "text-[#0277BD]"}`}>101</span>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* 5-Tab NavigationBar */}
              <div className={`h-14 border-t px-2 flex justify-around items-center transition-colors duration-300 ${
                isDark ? "bg-[#1E1E1E] border-[#2C2C2C]" : "bg-white border-[#E2E8F0]"
              }`}>
                <div
                  onClick={() => setSelectedPreviewScreen("home")}
                  className={`flex flex-col items-center gap-0.5 cursor-pointer ${
                    selectedPreviewScreen === "home" ? isDark ? "text-[#4FC3F7]" : "text-[#0277BD]" : "text-zinc-400"
                  }`}
                >
                  <HomeIcon className="w-4 h-4" />
                  <span className="text-[8px] font-bold">Home</span>
                </div>
                <div className="flex flex-col items-center gap-0.5 text-zinc-400 cursor-pointer">
                  <Calendar className="w-4 h-4" />
                  <span className="text-[8px] font-medium">Schedule</span>
                </div>
                <div className="flex flex-col items-center -mt-3 cursor-pointer">
                  <div className="w-8 h-8 rounded-full bg-[#0277BD] text-white flex items-center justify-center shadow-md">
                    <Plus className="w-4 h-4" />
                  </div>
                  <span className="text-[8px] font-medium text-zinc-400">Add</span>
                </div>
                <div className="flex flex-col items-center gap-0.5 text-zinc-400 cursor-pointer">
                  <Pill className="w-4 h-4" />
                  <span className="text-[8px] font-medium">Medicines</span>
                </div>
                <div
                  onClick={() => setSelectedPreviewScreen(selectedPreviewScreen === "emergency" ? "home" : "emergency")}
                  className={`flex flex-col items-center gap-0.5 cursor-pointer ${
                    selectedPreviewScreen === "emergency" ? "text-red-500" : "text-zinc-400"
                  }`}
                >
                  <PhoneCall className="w-4 h-4" />
                  <span className="text-[8px] font-medium">Emergency</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 12-Language Showcase */}
      <section id="languages" className={`max-w-7xl mx-auto px-6 py-24 border-t ${
        isDark ? "border-[#2C2C2C]" : "border-[#E2E8F0]"
      }`}>
        <div className={`border rounded-[40px] p-8 md:p-16 grid md:grid-cols-12 gap-12 items-center ${
          isDark ? "bg-[#1E1E1E] border-[#2C2C2C]" : "bg-white border-[#E2E8F0] shadow-lg"
        }`}>
          <div className="md:col-span-5 flex flex-col items-start gap-6">
            <div className={`w-12 h-12 rounded-2xl flex items-center justify-center border ${
              isDark ? "bg-[#2C2C2C] border-[#333333]" : "bg-[#0277BD]/10 border-[#0277BD]/20"
            }`}>
              <Languages className="w-6 h-6 text-[#0277BD] dark:text-[#4FC3F7]" />
            </div>
            <h2 className="text-3xl sm:text-5xl font-extrabold tracking-tight">
              12 Global Languages
            </h2>
            <p className="text-zinc-500 dark:text-zinc-400 text-sm leading-relaxed">
              Dosezy includes hand-verified translations. Select any language below to preview localized string outputs:
            </p>

            {/* Language grid picker */}
            <div className="grid grid-cols-3 gap-2 w-full">
              {Object.keys(translations).map((lang) => (
                <button
                  key={lang}
                  onClick={() => setSelectedLanguage(lang)}
                  className={`py-2 rounded-xl text-xs font-bold border transition-all ${
                    selectedLanguage === lang
                      ? "bg-[#0277BD] text-white border-[#4FC3F7]"
                      : isDark
                        ? "bg-[#2C2C2C] border-transparent text-zinc-400 hover:text-white"
                        : "bg-zinc-100 border-transparent text-zinc-700 hover:bg-zinc-200"
                  }`}
                >
                  {translations[lang].label}
                </button>
              ))}
            </div>
          </div>

          <div className={`md:col-span-7 rounded-3xl p-8 flex flex-col gap-6 border ${
            isDark ? "bg-[#121212] border-[#2C2C2C]" : "bg-[#F8F9FA] border-[#E2E8F0]"
          }`}>
            <div className="flex items-center gap-3 border-b border-zinc-500/20 pb-4">
              <span className="text-xs font-mono text-zinc-500">RESOURCE KEY:</span>
              <span className="text-xs font-bold px-2 py-0.5 rounded-md bg-[#0277BD]/15 text-[#0277BD] dark:text-[#4FC3F7]">
                values-{selectedLanguage}/strings.xml
              </span>
            </div>

            <div className="flex flex-col gap-4 text-left">
              <div>
                <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">Banner Description</span>
                <p className="text-xl font-bold mt-1 leading-snug">
                  "{translations[selectedLanguage].text}"
                </p>
              </div>

              <div>
                <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">Screen Header Title</span>
                <p className="text-base font-bold mt-1 opacity-80">
                  "{translations[selectedLanguage].emergency}"
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* FAQs */}
      <section id="faqs" className={`max-w-4xl mx-auto px-6 py-24 border-t ${
        isDark ? "border-[#2C2C2C]" : "border-[#E2E8F0]"
      }`}>
        <div className="text-center mb-16">
          <h2 className="text-3xl sm:text-5xl font-extrabold tracking-tight">
            Frequently Asked Questions
          </h2>
          <p className="text-zinc-500 dark:text-zinc-400 mt-4">
            Details about offline security, permissions, and theme design.
          </p>
        </div>

        <div className="flex flex-col gap-4">
          {faqs.map((faq, idx) => (
            <div
              key={idx}
              className={`rounded-2xl border transition-all overflow-hidden ${
                isDark ? "bg-[#1E1E1E] border-[#2C2C2C]" : "bg-white border-[#E2E8F0] shadow-sm"
              }`}
            >
              <button
                onClick={() => setActiveFaq(activeFaq === idx ? null : idx)}
                className="w-full flex items-center justify-between p-6 text-left"
              >
                <span className="font-bold text-base md:text-lg flex items-center gap-3">
                  <HelpCircle className="w-5 h-5 text-[#0277BD] dark:text-[#4FC3F7] flex-shrink-0" />
                  {faq.q}
                </span>
                <ChevronDown
                  className={`w-5 h-5 text-zinc-400 transition-transform duration-200 ${
                    activeFaq === idx ? "rotate-180" : ""
                  }`}
                />
              </button>
              {activeFaq === idx && (
                <div className="px-6 pb-6 text-sm text-zinc-500 dark:text-zinc-400 leading-relaxed border-t border-zinc-500/10 pt-4">
                  {faq.a}
                </div>
              )}
            </div>
          ))}
        </div>
      </section>

      {/* Download Banner */}
      <section className={`border-t py-24 relative overflow-hidden ${
        isDark ? "bg-[#161616] border-[#2C2C2C]" : "bg-[#F1F5F9] border-[#E2E8F0]"
      }`}>
        <div className="max-w-4xl mx-auto px-6 text-center flex flex-col items-center gap-8 relative z-10">
          <h2 className="text-4xl sm:text-6xl font-extrabold tracking-tight">
            Take Control of Your Schedule
          </h2>
          <p className="text-zinc-500 dark:text-zinc-400 text-lg max-w-xl leading-relaxed">
            Download the latest release and run it natively on your Android device (v7.0+). Completely open source, private, and ad-free.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 justify-center w-full sm:w-auto">
            <a
              href="https://github.com/saad2134/dosezy/releases"
              target="_blank"
              rel="noreferrer"
              className="flex items-center justify-center gap-3 px-8 py-4 rounded-full bg-gradient-to-r from-[#0277BD] to-[#4FC3F7] text-white font-bold shadow-xl shadow-[#0277BD]/20 hover:scale-[1.01] transition-all"
            >
              <Download className="w-5 h-5" />
              Download APK (v2.1.0)
            </a>
            <a
              href="https://github.com/saad2134/dosezy"
              target="_blank"
              rel="noreferrer"
              className={`flex items-center justify-center gap-3 px-8 py-4 rounded-full font-bold border transition-all ${
                isDark
                  ? "bg-[#1E1E1E] border-[#2C2C2C] text-[#E0E0E0] hover:border-[#4FC3F7]/40"
                  : "bg-white border-[#E2E8F0] text-[#1E293B] hover:border-[#0277BD]/40 shadow-sm"
              }`}
            >
              GitHub Source Code
            </a>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className={`border-t py-12 text-center text-xs ${
        isDark ? "bg-[#121212] border-[#2C2C2C] text-zinc-500" : "bg-white border-[#E2E8F0] text-zinc-500"
      }`}>
        <div className="max-w-7xl mx-auto px-6 flex flex-col sm:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-2">
            <span className="text-base">💊</span>
            <span className="font-semibold">Dosezy App</span>
            <span className="opacity-40">|</span>
            <span>Simplifying health for everyone.</span>
          </div>

          <div className="flex items-center gap-6">
            <a href="https://github.com/saad2134/dosezy" target="_blank" rel="noreferrer" className="hover:underline">
              Source Code
            </a>
            <a href="https://github.com/saad2134/dosezy/issues" target="_blank" rel="noreferrer" className="hover:underline">
              Report Issue
            </a>
            <span>License: MIT</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
