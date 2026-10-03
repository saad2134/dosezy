import { useState, useMemo } from 'react';
import { CaregiverPatientSummary, CaregiverRole } from '../api/types';
import { MaterialIcon } from './MaterialIcon';

interface PatientSidebarProps {
  patients: CaregiverPatientSummary[];
  activePatient: CaregiverPatientSummary | null;
  onSelectPatient: (patient: CaregiverPatientSummary) => void;
  onOpenPairModal: () => void;
  isOpenMobile: boolean;
  onCloseMobile: () => void;
  caregiverName: string;
  caregiverEmail: string;
  caregiverRole?: CaregiverRole;
  specialty?: string | null;
  clinicName?: string | null;
  onLogout: () => void;
}

export const PatientSidebar: React.FC<PatientSidebarProps> = ({
  patients,
  activePatient,
  onSelectPatient,
  onOpenPairModal,
  isOpenMobile,
  onCloseMobile,
  caregiverName,
  caregiverEmail,
  caregiverRole = 'family',
  specialty,
  clinicName,
  onLogout,
}) => {
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<'all' | 'needs_attention'>('all');

  const isDoctor = caregiverRole === 'doctor';

  const filteredPatients = useMemo(() => {
    return patients.filter((p) => {
      const matchSearch = p.fullName.toLowerCase().includes(search.toLowerCase());
      if (filter === 'needs_attention') {
        return matchSearch && (p.missedDosesCount > 0 || p.adherenceRate < 80);
      }
      return matchSearch;
    });
  }, [patients, search, filter]);

  const caregiverInitials = caregiverName
    .split(' ')
    .map(n => n[0])
    .join('')
    .slice(0, 2)
    .toUpperCase() || (isDoctor ? 'DR' : 'CG');

  const handleSelect = (patient: CaregiverPatientSummary) => {
    onSelectPatient(patient);
    onCloseMobile();
  };

  const handlePair = () => {
    onOpenPairModal();
    onCloseMobile();
  };

  return (
    <>
      {/* Mobile Backdrop */}
      {isOpenMobile && (
        <div
          onClick={onCloseMobile}
          className="fixed inset-0 bg-black/70 backdrop-blur-sm z-40 md:hidden animate-in fade-in duration-200"
        />
      )}

      {/* Primary Leftmost Sidebar Column */}
      <aside
        className={`fixed md:static inset-y-0 left-0 z-50 w-72 sm:w-80 bg-[#070b14] border-r border-white/10 flex flex-col transition-transform duration-300 ease-in-out shrink-0 ${
          isOpenMobile ? 'translate-x-0 shadow-2xl' : '-translate-x-full md:translate-x-0'
        }`}
      >
        {/* Brand Header & Mobile Close */}
        <div className="p-4 border-b border-white/10 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2.5">
              <div className="w-8 h-8 rounded-xl p-[2px] bg-gradient-to-tr from-sky-600 via-blue-500 to-cyan-400 shrink-0 shadow-md">
                <img
                  src="/icon-squircle-1000px.png"
                  alt="Dosezy"
                  className="w-full h-full object-cover rounded-[10px]"
                />
              </div>
              <div>
                <span className="font-extrabold text-sm font-display tracking-tight text-white block leading-none">
                  {isDoctor ? 'Clinical Roster' : 'Dosezy Portal'}
                </span>
                <span className="text-[10px] text-sky-400 font-mono block mt-1 leading-none font-semibold">
                  {isDoctor ? 'Physician Review' : 'Family Care'}
                </span>
              </div>
            </div>

            {isOpenMobile && (
              <button
                onClick={onCloseMobile}
                className="p-1.5 rounded-xl text-neutral-400 hover:text-white hover:bg-white/10 md:hidden transition-all"
                title="Close"
              >
                <MaterialIcon name="close" className="text-lg" />
              </button>
            )}
          </div>

          {/* Search Input */}
          <div className="relative flex items-center">
            <span className="absolute left-3 flex items-center justify-center pointer-events-none text-neutral-400 z-10">
              <MaterialIcon name="search" className="!text-base leading-none" />
            </span>
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder={isDoctor ? "Search patient records..." : "Search family..."}
              className="w-full pl-9 pr-8 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-sky-500 transition-all"
            />
            {search && (
              <button
                type="button"
                onClick={() => setSearch('')}
                className="absolute right-2.5 p-1 rounded-lg text-neutral-400 hover:text-white hover:bg-white/10 transition-all z-10"
                title="Clear search"
              >
                <MaterialIcon name="close" className="!text-sm leading-none" />
              </button>
            )}
          </div>

          {/* Patient Header & Filter Pills */}
          <div className="flex items-center justify-between pt-1">
            <div className="flex items-center space-x-1.5 text-[10px] font-semibold">
              <button
                onClick={() => setFilter('all')}
                className={`px-2.5 py-1 rounded-lg transition-all ${
                  filter === 'all'
                    ? 'bg-sky-600/20 text-sky-300 border border-sky-500/30'
                    : 'text-neutral-400 hover:text-white'
                }`}
              >
                All ({patients.length})
              </button>
              <button
                onClick={() => setFilter('needs_attention')}
                className={`px-2.5 py-1 rounded-lg transition-all ${
                  filter === 'needs_attention'
                    ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                    : 'text-neutral-400 hover:text-white'
                }`}
              >
                Needs Review
              </button>
            </div>

            <span className="text-[10px] font-mono text-neutral-400">
              {filteredPatients.length} shown
            </span>
          </div>
        </div>

        {/* Patient List (Scrollable) */}
        <div className="flex-grow overflow-y-auto p-3 space-y-2">
          {filteredPatients.length === 0 ? (
            <div className="py-12 text-center text-neutral-500 text-xs font-mono">
              {search ? 'No patients found matching query' : 'No patients paired yet'}
            </div>
          ) : (
            filteredPatients.map((p) => {
              const isSelected = activePatient?.userId === p.userId;
              const rate = p.adherenceRate || 0;
              const isLow = rate < 80;

              return (
                <button
                  key={p.userId}
                  onClick={() => handleSelect(p)}
                  className={`w-full p-3 rounded-2xl border text-left transition-all flex items-center justify-between group ${
                    isSelected
                      ? 'bg-sky-600/20 border-sky-500/50 shadow-md ring-1 ring-sky-500/30'
                      : 'bg-white/[0.02] border-white/5 hover:bg-white/[0.06] hover:border-white/10'
                  }`}
                >
                  <div className="flex items-center space-x-3 min-w-0">
                    {/* Patient Initials Circle */}
                    <div
                      className={`w-10 h-10 rounded-xl flex items-center justify-center font-bold text-xs shrink-0 transition-all ${
                        isSelected
                          ? 'bg-gradient-to-tr from-[#0277bd] to-[#2084e4] text-white shadow-sm'
                          : 'bg-white/10 text-neutral-300 group-hover:text-white'
                      }`}
                    >
                      {p.fullName
                        .split(' ')
                        .map(n => n[0])
                        .join('')
                        .slice(0, 2)
                        .toUpperCase()}
                    </div>

                    <div className="min-w-0">
                      <span className={`text-xs font-bold block truncate ${
                        isSelected ? 'text-white' : 'text-neutral-200 group-hover:text-white'
                      }`}>
                        {p.fullName}
                      </span>
                      <div className="flex items-center space-x-2 text-[10px] text-neutral-400 font-mono mt-0.5">
                        <span>{p.activeMedicinesCount} meds</span>
                        {p.missedDosesCount > 0 ? (
                          <span className="text-amber-400 font-semibold flex items-center space-x-0.5">
                            <span>•</span>
                            <span>{p.missedDosesCount} missed</span>
                          </span>
                        ) : (
                          <span className="text-emerald-400">
                            • {p.todayPendingCount} due
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Adherence Rate Badge */}
                  <span className={`px-2 py-0.5 rounded-full text-[10px] font-mono font-bold shrink-0 ml-2 ${
                    isLow
                      ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                      : 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                  }`}>
                    {rate}%
                  </span>
                </button>
              );
            })
          )}
        </div>

        {/* Bottom Actions: Pair New Patient + Caregiver Profile */}
        <div className="p-3 border-t border-white/10 space-y-2 safe-bottom bg-[#06080c]/50">
          <button
            onClick={handlePair}
            className="w-full py-2.5 px-3 rounded-xl bg-gradient-to-r from-[#0277bd] via-[#2084e4] to-[#0284c7] hover:from-[#01579b] hover:via-[#0277bd] hover:to-[#2084e4] text-white font-bold text-xs shadow-md hover:shadow-sky-500/20 transition-all flex items-center justify-center space-x-2 cursor-pointer"
          >
            <MaterialIcon name="person_add" className="text-base" />
            <span>{isDoctor ? 'Link Patient Chart' : 'Pair New Patient'}</span>
          </button>

          {/* Caregiver / Physician Account Card */}
          <div className="pt-2 border-t border-white/5 flex items-center justify-between px-1">
            <div className="flex items-center space-x-2.5 min-w-0">
              <div className={`w-8 h-8 rounded-xl flex items-center justify-center text-xs font-bold shrink-0 shadow-sm ${
                isDoctor
                  ? 'bg-gradient-to-br from-emerald-600 to-teal-800 text-white border border-emerald-400/40'
                  : 'bg-gradient-to-br from-[#0277bd] to-[#01579b] text-white border border-sky-400/30'
              }`}>
                {caregiverInitials}
              </div>
              <div className="min-w-0">
                <div className="flex items-center space-x-1.5">
                  <span className="text-xs font-bold text-white block truncate leading-tight">
                    {caregiverName}
                  </span>
                </div>
                <span className="text-[10px] text-neutral-400 block truncate font-mono">
                  {isDoctor ? (specialty ? `${specialty}${clinicName ? ' • ' + clinicName : ''}` : (clinicName || 'Clinical Doctor')) : caregiverEmail}
                </span>
              </div>
            </div>

            <button
              onClick={onLogout}
              title="Sign Out"
              className="p-1.5 rounded-xl text-neutral-400 hover:text-red-300 hover:bg-red-500/10 transition-all shrink-0 ml-2 cursor-pointer"
            >
              <MaterialIcon name="logout" className="text-base" />
            </button>
          </div>
        </div>
      </aside>
    </>
  );
};
