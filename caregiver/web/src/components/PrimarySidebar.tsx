import { MaterialIcon } from './MaterialIcon';

interface PrimarySidebarProps {
  activeTab: 'dashboard' | 'prescriptions' | 'analytics' | 'safety' | 'settings';
  setActiveTab: (tab: 'dashboard' | 'prescriptions' | 'analytics' | 'safety' | 'settings') => void;
  isConnected: boolean;
  caregiverName: string;
  onLogout: () => void;
}

export const PrimarySidebar: React.FC<PrimarySidebarProps> = ({
  activeTab,
  setActiveTab,
  isConnected,
  caregiverName,
  onLogout,
}) => {
  const navItems = [
    { id: 'dashboard' as const, label: 'Live Monitor', icon: 'dashboard' },
    { id: 'prescriptions' as const, label: 'Prescriptions', icon: 'medication' },
    { id: 'analytics' as const, label: 'Analytics', icon: 'monitoring' },
    { id: 'safety' as const, label: 'Safety Profile', icon: 'health_and_safety' },
    { id: 'settings' as const, label: 'Server Status', icon: 'dns' },
  ];

  const initials = caregiverName
    .split(' ')
    .map(n => n[0])
    .join('')
    .slice(0, 2)
    .toUpperCase() || 'CG';

  return (
    <aside className="hidden md:flex w-18 shrink-0 bg-[#06080c] border-r border-white/10 flex-col justify-between items-center py-5 z-30 select-none">
      
      {/* Top: App Brand Logo */}
      <div className="flex flex-col items-center space-y-6">
        <a href="/" className="group" title="Dosezy Caregiver Portal">
          <div className="w-10 h-10 rounded-2xl p-[1.5px] bg-gradient-to-tr from-blue-500 via-sky-400 to-indigo-500 group-hover:scale-105 transition-transform duration-300 shadow-[0_0_15px_rgba(2,132,199,0.3)]">
            <img src="/icon-squircle-1000px.png" alt="Dosezy Icon" className="w-full h-full object-cover rounded-[14px]" />
          </div>
        </a>

        {/* Primary Navigation Icons */}
        <nav className="flex flex-col space-y-2">
          {navItems.map((item) => {
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                title={item.label}
                className={`relative group w-11 h-11 rounded-2xl flex items-center justify-center transition-all ${
                  isActive
                    ? 'bg-sky-600 text-white shadow-lg shadow-sky-500/30'
                    : 'text-neutral-400 hover:text-white hover:bg-white/5'
                }`}
              >
                <MaterialIcon name={item.icon} filled={isActive} className="text-xl" />
                
                {/* Tooltip on hover */}
                <span className="absolute left-14 px-2.5 py-1 rounded-lg bg-[#0a0c10] border border-white/15 text-[11px] font-semibold text-white whitespace-nowrap opacity-0 pointer-events-none group-hover:opacity-100 transition-opacity z-50 shadow-xl">
                  {item.label}
                </span>

                {/* Active Indicator Bar */}
                {isActive && (
                  <span className="absolute -left-3.5 w-1 h-5 rounded-r-full bg-sky-400" />
                )}
              </button>
            );
          })}
        </nav>
      </div>

      {/* Bottom: Realtime Connection Pulse & Profile */}
      <div className="flex flex-col items-center space-y-4">
        
        {/* SSE Pulse Indicator */}
        <div
          title={isConnected ? 'Real-Time SSE Connected' : 'Reconnecting to Server'}
          className="w-8 h-8 rounded-full bg-white/[0.04] border border-white/10 flex items-center justify-center cursor-pointer"
        >
          <span className="relative flex h-2.5 w-2.5">
            {isConnected && (
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            )}
            <span className={`relative inline-flex rounded-full h-2.5 w-2.5 ${isConnected ? 'bg-emerald-500' : 'bg-amber-500'}`}></span>
          </span>
        </div>

        {/* Caregiver Profile Avatar & Logout */}
        <button
          onClick={onLogout}
          title={`Signed in as ${caregiverName} (Click to Sign Out)`}
          className="group relative w-10 h-10 rounded-2xl bg-gradient-to-br from-[#0277bd] to-[#01579b] border border-sky-400/30 flex items-center justify-center text-xs font-bold text-white shadow-md hover:border-sky-400 transition-all"
        >
          <span>{initials}</span>

          <span className="absolute left-14 px-2.5 py-1 rounded-lg bg-[#0a0c10] border border-white/15 text-[11px] font-semibold text-red-300 whitespace-nowrap opacity-0 pointer-events-none group-hover:opacity-100 transition-opacity z-50 shadow-xl">
            Sign Out
          </span>
        </button>

      </div>

    </aside>
  );
};
