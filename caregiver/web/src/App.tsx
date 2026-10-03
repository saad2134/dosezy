import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchPatients, fetchMedicines, fetchSchedules } from './api/client';
import { CaregiverAuth, CaregiverPatientSummary, CaregiverRole } from './api/types';
import { useRealtimeEvents } from './hooks/useRealtimeEvents';
import { PatientSidebar } from './components/PatientSidebar';
import { PatientPairModal } from './components/PatientPairModal';
import { AuthScreen } from './components/AuthScreen';
import { DashboardPage } from './pages/DashboardPage';
import { PrescriptionsPage } from './pages/PrescriptionsPage';
import { AnalyticsPage } from './pages/AnalyticsPage';
import { SafetyProfilePage } from './pages/SafetyProfilePage';
import { SettingsPage } from './pages/SettingsPage';
import { ThemeSwitcher } from './components/ThemeSwitcher';
import { MaterialIcon } from './components/MaterialIcon';

export function App() {
  // Auth state with dual-role persona
  const [caregiver, setCaregiver] = useState<CaregiverAuth | null>(() => {
    const id = localStorage.getItem('dosezy_caregiver_id');
    const name = localStorage.getItem('dosezy_caregiver_name');
    const email = localStorage.getItem('dosezy_caregiver_email');
    const role = (localStorage.getItem('dosezy_caregiver_role') as CaregiverRole) || 'family';
    const specialty = localStorage.getItem('dosezy_caregiver_specialty');
    const clinicName = localStorage.getItem('dosezy_caregiver_clinic');
    if (id && name && email) return { id, name, email, role, specialty, clinicName };
    return null;
  });

  const isDoctor = caregiver?.role === 'doctor';

  // Navigation state (Doctors default to Clinical Analytics, Family defaults to Live Monitor)
  const [activeTab, setActiveTab] = useState<'dashboard' | 'prescriptions' | 'analytics' | 'safety' | 'settings'>(
    () => ((localStorage.getItem('dosezy_caregiver_role') === 'doctor') ? 'analytics' : 'dashboard')
  );
  const [activePatient, setActivePatient] = useState<CaregiverPatientSummary | null>(null);
  const [isPairModalOpen, setIsPairModalOpen] = useState(false);
  const [isPatientDrawerOpen, setIsPatientDrawerOpen] = useState(false);
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);

  // Real-time SSE Connection
  const { isConnected } = useRealtimeEvents();

  // Queries
  const { data: patients = [] } = useQuery({
    queryKey: ['patients'],
    queryFn: fetchPatients,
    enabled: Boolean(caregiver),
  });

  const { data: medicines = [] } = useQuery({
    queryKey: ['medicines'],
    queryFn: fetchMedicines,
    enabled: Boolean(caregiver),
  });

  const { data: schedules = [] } = useQuery({
    queryKey: ['schedules'],
    queryFn: fetchSchedules,
    enabled: Boolean(caregiver),
  });

  // Default patient selection
  useEffect(() => {
    if (patients.length > 0 && !activePatient) {
      setActivePatient(patients[0]);
    } else if (patients.length > 0 && activePatient) {
      const updated = patients.find(p => p.userId === activePatient.userId);
      if (updated) setActivePatient(updated);
    }
  }, [patients, activePatient]);

  const handleLogout = () => {
    localStorage.removeItem('dosezy_caregiver_id');
    localStorage.removeItem('dosezy_caregiver_name');
    localStorage.removeItem('dosezy_caregiver_email');
    localStorage.removeItem('dosezy_caregiver_role');
    localStorage.removeItem('dosezy_caregiver_specialty');
    localStorage.removeItem('dosezy_caregiver_clinic');
    localStorage.removeItem('dosezy_demo_mode');
    setCaregiver(null);
    setIsProfileMenuOpen(false);
  };

  // Strictly require authentication
  if (!caregiver) {
    return (
      <AuthScreen
        onAuthenticated={(cg) => {
          setCaregiver(cg);
          setActiveTab(cg.role === 'doctor' ? 'analytics' : 'dashboard');
        }}
      />
    );
  }

  const caregiverName = caregiver.name;
  const caregiverEmail = caregiver.email;
  const caregiverRole = caregiver.role;
  const specialty = caregiver.specialty;
  const clinicName = caregiver.clinicName;

  const desktopNavTabs = isDoctor
    ? [
        { id: 'analytics' as const, label: 'Clinical Audit & Trends', icon: 'monitoring' },
        { id: 'prescriptions' as const, label: 'Prescription Regimens', icon: 'medication' },
        { id: 'safety' as const, label: 'Allergies & Safety', icon: 'health_and_safety' },
        { id: 'dashboard' as const, label: 'Dose Timeline', icon: 'event_repeat' },
        { id: 'settings' as const, label: 'Server & Security', icon: 'dns' },
      ]
    : [
        { id: 'dashboard' as const, label: 'Live Monitor', icon: 'dashboard' },
        { id: 'prescriptions' as const, label: 'Prescriptions & Stock', icon: 'medication' },
        { id: 'analytics' as const, label: 'Adherence Analytics', icon: 'monitoring' },
        { id: 'safety' as const, label: 'Safety Profile', icon: 'health_and_safety' },
        { id: 'settings' as const, label: 'Server & Security', icon: 'dns' },
      ];

  return (
    <div className="h-screen w-screen bg-[#030712] text-white flex overflow-hidden font-sans relative">
      
      {/* Ambient Blue Radial Spotlights matching Dosezy Android Theme */}
      <div className="ambient-glow-blue-primary" />
      <div className="ambient-glow-blue-cyan" />

      {/* 1. Left Column: Patient Selection (Primary Leftmost Column) */}
      <PatientSidebar
        patients={patients}
        activePatient={activePatient}
        onSelectPatient={setActivePatient}
        onOpenPairModal={() => setIsPairModalOpen(true)}
        isOpenMobile={isPatientDrawerOpen}
        onCloseMobile={() => setIsPatientDrawerOpen(false)}
        caregiverName={caregiverName}
        caregiverEmail={caregiverEmail}
        caregiverRole={caregiverRole}
        specialty={specialty}
        clinicName={clinicName}
        onLogout={handleLogout}
      />

      {/* 2. Main Patient Canvas (Tabs Across the Top) */}
      <div className="flex-1 flex flex-col min-w-0 h-full overflow-hidden relative z-10">
        
        {/* Desktop Header: Patient Info + Horizontal Navigation Tabs */}
        <header className="hidden md:flex flex-col shrink-0 border-b border-white/10 bg-[#070b14]/80 backdrop-blur-xl">
          {/* Top Row: Patient Info & Clinical Mode Indicator */}
          <div className="h-16 px-6 lg:px-8 flex items-center justify-between border-b border-white/5">
            <div className="flex items-center space-x-3">
              <h1 className="text-xl font-bold text-white font-display">
                {activePatient ? activePatient.fullName : (isDoctor ? 'Clinical Review Portal' : 'Caregiver Portal')}
              </h1>
              {activePatient && (
                <span className="px-2.5 py-0.5 rounded-full text-xs font-mono font-bold bg-sky-500/10 text-sky-300 border border-sky-500/20">
                  {activePatient.adherenceRate}% Adherence Score
                </span>
              )}
              {isDoctor ? (
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold bg-emerald-500/15 text-emerald-300 border border-emerald-500/30 flex items-center space-x-1">
                  <span>🩺</span>
                  <span>Doctor Review (Read-Only)</span>
                </span>
              ) : (
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold bg-sky-500/15 text-sky-300 border border-sky-500/30 flex items-center space-x-1">
                  <span>👨‍👩‍👧</span>
                  <span>Family Oversight</span>
                </span>
              )}
            </div>

            {/* Right Side: Status, Clinical Print & Theme */}
            <div className="flex items-center space-x-3">
              
              {/* Doctor Quick Print Button */}
              {isDoctor && (
                <button
                  onClick={() => window.print()}
                  className="px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/15 text-white text-xs font-bold border border-white/10 flex items-center space-x-1.5 transition-all shadow-sm cursor-pointer"
                  title="Export official doctor A4 printable medical chart"
                >
                  <MaterialIcon name="print" className="text-sm text-sky-400" />
                  <span>Export Doctor A4 PDF</span>
                </button>
              )}

              {/* Alert Mode Indicator */}
              {isDoctor ? (
                <div
                  className="flex items-center space-x-1.5 px-3 py-1 rounded-full bg-white/5 border border-white/10 text-xs font-mono text-neutral-300"
                  title="Clinical accounts receive longitudinal digests and are spared off-hours emergency alarms."
                >
                  <MaterialIcon name="notifications_off" className="text-sm text-neutral-400" />
                  <span className="text-[11px]">Digest Mode (Muted Alarms)</span>
                </div>
              ) : (
                <div className="flex items-center space-x-2 px-3 py-1 rounded-full bg-white/5 border border-white/10 text-xs font-mono text-neutral-300">
                  <span className="relative flex h-2 w-2">
                    {isConnected && (
                      <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                    )}
                    <span className={`relative inline-flex rounded-full h-2 w-2 ${isConnected ? 'bg-emerald-400' : 'bg-amber-400'}`}></span>
                  </span>
                  <span>{isConnected ? 'Real-Time Sync Active' : 'Connecting...'}</span>
                </div>
              )}

              <ThemeSwitcher />
            </div>
          </div>

          {/* Bottom Row: Desktop Horizontal Navigation Tabs */}
          <div className="px-6 lg:px-8 flex items-center space-x-2 py-2 overflow-x-auto">
            {desktopNavTabs.map((tab) => {
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center space-x-2 shrink-0 cursor-pointer ${
                    isActive
                      ? 'bg-sky-600/20 text-sky-300 border border-sky-500/40 shadow-sm'
                      : 'text-neutral-400 hover:text-white hover:bg-white/5 border border-transparent'
                  }`}
                >
                  <MaterialIcon
                    name={tab.icon}
                    filled={isActive}
                    className={`text-base ${isActive ? 'text-sky-400' : 'text-neutral-400'}`}
                  />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </div>
        </header>

        {/* Mobile Top Header */}
        <header className="md:hidden shrink-0 border-b border-white/10 bg-[#070b14]/90 backdrop-blur-xl px-4 py-3 flex items-center justify-between">
          <div className="flex items-center space-x-2.5">
            <button
              onClick={() => setIsPatientDrawerOpen(true)}
              className="p-2 rounded-xl bg-white/5 border border-white/10 text-white"
              title="Open Patients Drawer"
            >
              <MaterialIcon name="menu" className="text-xl" />
            </button>
            <div>
              <span className="text-xs font-bold text-white block truncate">
                {activePatient ? activePatient.fullName : (isDoctor ? 'Clinical Review' : 'Caregiver Portal')}
              </span>
              <span className="text-[10px] text-sky-400 font-mono block">
                {isDoctor ? '🩺 Doctor Mode' : '👨‍👩‍👧 Family Mode'}
              </span>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <ThemeSwitcher />
            <button
              onClick={() => setIsProfileMenuOpen(!isProfileMenuOpen)}
              className="w-8 h-8 rounded-xl bg-sky-500/20 border border-sky-500/30 text-sky-300 text-xs font-bold flex items-center justify-center"
            >
              {isDoctor ? 'DR' : 'CG'}
            </button>
          </div>
        </header>

        {/* Mobile Dropdown Menu for Profile */}
        {isProfileMenuOpen && (
          <div className="md:hidden absolute right-4 top-16 w-56 rounded-2xl bg-[#0a101d] border border-white/15 p-2 shadow-2xl z-50 backdrop-blur-2xl">
            <div className="p-2 border-b border-white/5 mb-1">
              <span className="text-xs font-bold text-white block">{caregiverName}</span>
              <span className="text-[10px] text-neutral-400 block font-mono">{caregiverEmail}</span>
              <span className="text-[10px] text-sky-400 block font-mono mt-0.5">
                {isDoctor ? `🩺 ${specialty || 'Doctor'}` : '👨‍👩‍👧 Family Member'}
              </span>
            </div>
            <button
              onClick={handleLogout}
              className="w-full p-2 rounded-xl text-left text-xs text-red-300 hover:bg-red-500/10 flex items-center space-x-2 transition-all"
            >
              <MaterialIcon name="logout" className="text-sm" />
              <span>Sign Out</span>
            </button>
          </div>
        )}

        {/* Main Content Area (Scrollable Tab Canvas) */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8 relative">
          <div className="max-w-6xl mx-auto pb-24 md:pb-8">
            {activeTab === 'dashboard' && (
              <DashboardPage
                patient={activePatient}
                medicines={medicines}
                schedules={schedules}
                onOpenPairModal={() => setIsPairModalOpen(true)}
              />
            )}

            {activeTab === 'prescriptions' && (
              <PrescriptionsPage
                patient={activePatient}
                medicines={medicines}
              />
            )}

            {activeTab === 'analytics' && (
              <AnalyticsPage
                patient={activePatient}
                medicines={medicines}
                schedules={schedules}
                isDoctor={isDoctor}
                caregiverName={caregiverName}
                specialty={specialty}
                clinicName={clinicName}
              />
            )}

            {activeTab === 'safety' && (
              <SafetyProfilePage
                patient={activePatient}
                medicines={medicines}
              />
            )}

            {activeTab === 'settings' && (
              <SettingsPage
                isConnected={isConnected}
              />
            )}
          </div>
        </main>

        {/* Mobile Bottom Navigation Bar */}
        <nav className="md:hidden shrink-0 border-t border-white/10 bg-[#070b14]/95 backdrop-blur-xl px-2 py-1 safe-bottom grid grid-cols-5 gap-1">
          {desktopNavTabs.map((tab) => {
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`py-1.5 flex flex-col items-center justify-center rounded-xl transition-all ${
                  isActive ? 'text-sky-400 bg-sky-500/10' : 'text-neutral-400 hover:text-white'
                }`}
              >
                <MaterialIcon name={tab.icon} filled={isActive} className="text-xl leading-none" />
                <span className="text-[9px] font-medium mt-1 truncate max-w-full">
                  {tab.id === 'dashboard' ? (isDoctor ? 'Timeline' : 'Monitor') : tab.id.charAt(0).toUpperCase() + tab.id.slice(1)}
                </span>
              </button>
            );
          })}
        </nav>

      </div>

      {/* Patient Pairing Modal */}
      <PatientPairModal
        isOpen={isPairModalOpen}
        onClose={() => setIsPairModalOpen(false)}
      />

    </div>
  );
}
