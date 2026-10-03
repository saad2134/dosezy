
import { CaregiverPatientSummary } from '../api/types';
import { Plus } from 'lucide-react';

interface NavbarProps {
  patients: CaregiverPatientSummary[];
  activePatient: CaregiverPatientSummary | null;
  onSelectPatient: (patient: CaregiverPatientSummary) => void;
  onOpenPairModal: () => void;
  isConnected: boolean;
  activeTab: 'dashboard' | 'analytics' | 'settings';
  setActiveTab: (tab: 'dashboard' | 'analytics' | 'settings') => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  patients,
  activePatient,
  onSelectPatient,
  onOpenPairModal,
  isConnected,
  activeTab,
  setActiveTab,
}) => {
  return (
    <header className="sticky top-0 z-40 w-full border-b border-white/10 bg-[#030407]/80 backdrop-blur-xl">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        
        {/* Brand Logo */}
        <div className="flex items-center space-x-6">
          <div className="flex items-center space-x-3">
            <div className="w-9 h-9 rounded-xl overflow-hidden p-[1.5px] bg-gradient-to-tr from-blue-500 via-sky-400 to-indigo-500 shadow-[0_0_15px_rgba(2,132,199,0.3)]">
              <img src="/icon-squircle-1000px.png" alt="Dosezy Icon" className="w-full h-full object-cover rounded-[9px]" />
            </div>
            <div>
              <div className="flex items-center space-x-1.5">
                <span className="font-extrabold text-base tracking-tight text-white font-display">
                  Dosezy<span className="text-sky-400">.</span>
                </span>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-sky-500/10 text-sky-300 border border-sky-500/20">
                  CAREGIVER
                </span>
              </div>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center space-x-1">
            <button
              onClick={() => setActiveTab('dashboard')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'dashboard'
                  ? 'bg-white/10 text-white shadow-sm'
                  : 'text-neutral-400 hover:text-white hover:bg-white/5'
              }`}
            >
              Live Monitor
            </button>
            <button
              onClick={() => setActiveTab('analytics')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'analytics'
                  ? 'bg-white/10 text-white shadow-sm'
                  : 'text-neutral-400 hover:text-white hover:bg-white/5'
              }`}
            >
              Adherence Analytics
            </button>
            <button
              onClick={() => setActiveTab('settings')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'settings'
                  ? 'bg-white/10 text-white shadow-sm'
                  : 'text-neutral-400 hover:text-white hover:bg-white/5'
              }`}
            >
              Server Status
            </button>
          </nav>
        </div>

        {/* Right Section: Patient Switcher, Live Connection, Pair Button */}
        <div className="flex items-center space-x-3">
          
          {/* Real-time SSE Connection Indicator */}
          <div className="hidden sm:flex items-center space-x-2 px-3 py-1 rounded-full bg-white/5 border border-white/10 text-[11px] font-mono">
            <span className="relative flex h-2 w-2">
              {isConnected && (
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              )}
              <span className={`relative inline-flex rounded-full h-2 w-2 ${isConnected ? 'bg-emerald-500' : 'bg-amber-500'}`}></span>
            </span>
            <span className={isConnected ? 'text-neutral-300' : 'text-amber-400'}>
              {isConnected ? 'Real-Time SSE Connected' : 'Reconnecting...'}
            </span>
          </div>

          {/* Patient Selector Dropdown */}
          {patients.length > 0 && (
            <div className="relative">
              <select
                aria-label="Select Monitored Patient"
                value={activePatient?.userId || ''}
                onChange={(e) => {
                  const p = patients.find(p => p.userId === e.target.value);
                  if (p) onSelectPatient(p);
                }}
                className="bg-[#0a0c10] border border-white/10 text-white text-xs font-medium rounded-xl px-3 py-1.5 pr-8 focus:outline-none focus:border-sky-500 appearance-none cursor-pointer"
              >
                {patients.map((p) => (
                  <option key={p.userId} value={p.userId}>
                    ?? {p.fullName} ({p.adherenceRate}%)
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Pair Patient Button */}
          <button
            onClick={onOpenPairModal}
            className="flex items-center space-x-1.5 px-3.5 py-1.5 rounded-xl text-xs font-bold text-white bg-gradient-to-r from-[#0277bd] via-[#2084e4] to-[#0284c7] hover:from-[#01579b] hover:via-[#0277bd] hover:to-[#2084e4] shadow-md hover:shadow-sky-500/20 active:scale-95 transition-all"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Pair Patient</span>
          </button>
        </div>

      </div>
    </header>
  );
};
