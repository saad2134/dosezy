import { CaregiverPatientSummary, Medicine, ScheduleEntry } from '../api/types';
import { MaterialIcon } from '../components/MaterialIcon';

interface AnalyticsPageProps {
  patient: CaregiverPatientSummary | null;
  medicines: Medicine[];
  schedules: ScheduleEntry[];
  isDoctor?: boolean;
  caregiverName?: string;
  specialty?: string | null;
  clinicName?: string | null;
}

export const AnalyticsPage: React.FC<AnalyticsPageProps> = ({
  patient,
  medicines,
  schedules,
  isDoctor = false,
  caregiverName,
  specialty,
  clinicName,
}) => {
  if (!patient) {
    return (
      <div className="py-20 text-center text-neutral-400 text-sm">
        Select a patient to view analytics.
      </div>
    );
  }

  const patientMeds = medicines.filter(m => m.userId === patient.userId);
  const patientSchedules = schedules.filter(s => s.userId === patient.userId);

  const daysOfWeek = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  const mockWeeklyRates = [100, 100, 85, 100, 92, 100, 94];

  // Clinical skip reasons breakdown
  const skipReasons = [
    { label: 'Doctor-Advised Pause', count: 7, pct: 58, icon: 'medical_services', color: 'text-sky-400', bg: 'bg-sky-500/20' },
    { label: 'Adverse Drug Reaction', count: 3, pct: 25, icon: 'warning', color: 'text-amber-400', bg: 'bg-amber-500/20' },
    { label: 'Fasting / Medical Procedure', count: 1, pct: 9, icon: 'restaurant', color: 'text-purple-400', bg: 'bg-purple-500/20' },
    { label: 'Acute Illness / Nausea', count: 1, pct: 8, icon: 'sick', color: 'text-teal-400', bg: 'bg-teal-500/20' },
  ];

  const adherenceScore = patient.adherenceRate || 0;
  const isOptimal = adherenceScore >= 80;
  const isModerate = adherenceScore >= 50 && adherenceScore < 80;

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="space-y-8 print:p-6 print:bg-white print:text-black">
      
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <span className="text-[11px] font-mono font-bold text-sky-400 uppercase tracking-wider">
              {isDoctor ? '🩺 Official Physician Adherence Audit' : 'Clinical Adherence Reports'}
            </span>
            {isDoctor && (
              <span className="px-2 py-0.5 rounded-full text-[10px] font-mono bg-emerald-500/15 text-emerald-300 border border-emerald-500/30 font-bold">
                Read-Only Medical Chart
              </span>
            )}
          </div>
          <h2 className="text-2xl font-bold text-white print:text-black font-display mt-0.5">
            {patient.fullName} — Longitudinal Compliance
          </h2>
          {isDoctor && caregiverName && (
            <p className="text-xs text-neutral-400 print:text-neutral-600 mt-1">
              Reviewing Clinician: <span className="text-white print:text-black font-semibold">{caregiverName}</span>
              {specialty && ` (${specialty})`}
              {clinicName && ` • ${clinicName}`}
            </p>
          )}
        </div>

        <button
          onClick={handlePrint}
          className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-[#0277bd] via-[#2084e4] to-[#0284c7] hover:from-[#01579b] text-white font-bold text-xs shadow-md hover:shadow-sky-500/20 flex items-center space-x-2 transition-all print:hidden cursor-pointer"
        >
          <MaterialIcon name="print" className="text-base" />
          <span>{isDoctor ? 'Export Doctor A4 PDF' : 'Print Medical Summary'}</span>
        </button>
      </div>

      {/* Clinical Risk Stratification Banner */}
      <div className={`p-4 sm:p-5 rounded-2xl border flex items-center justify-between gap-4 ${
        isOptimal
          ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-200'
          : isModerate
          ? 'bg-amber-500/10 border-amber-500/20 text-amber-200'
          : 'bg-red-500/10 border-red-500/20 text-red-200'
      }`}>
        <div className="flex items-center space-x-3">
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
            isOptimal ? 'bg-emerald-500/20 text-emerald-400' : isModerate ? 'bg-amber-500/20 text-amber-400' : 'bg-red-500/20 text-red-400'
          }`}>
            <MaterialIcon name={isOptimal ? 'verified' : isModerate ? 'help_outline' : 'error'} filled className="text-xl" />
          </div>
          <div>
            <span className="font-bold text-sm block">
              {isOptimal ? 'Optimal Clinical Compliance (≥ 80%)' : isModerate ? 'Moderate Compliance (50% - 79%)' : 'Critical Non-Compliance Risk (< 50%)'}
            </span>
            <span className="text-xs text-neutral-400 print:text-neutral-600 block mt-0.5">
              {isOptimal
                ? 'Patient exhibits strong regimen consistency. Therapeutic blood levels likely stable.'
                : isModerate
                ? 'Periodic missed or late doses detected. Routine consultation advised.'
                : 'Frequent missed doses. Immediate clinical review or caregiver intervention required.'}
            </span>
          </div>
        </div>

        <div className="text-right shrink-0">
          <span className="text-2xl font-black font-display block text-white print:text-black">
            {adherenceScore}%
          </span>
          <span className="text-[10px] font-mono text-neutral-400 uppercase">30-Day Index</span>
        </div>
      </div>

      {/* 7-Day Compliance Heatmap */}
      <div className="glass-card p-6 sm:p-8 rounded-3xl print:border print:border-neutral-300">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-2">
            <MaterialIcon name="calendar_month" filled className="text-xl text-sky-400" />
            <h3 className="text-base font-bold text-white print:text-black font-display">
              Weekly Adherence Heatmap & Intake Rhythm
            </h3>
          </div>
          <span className="text-xs font-mono text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-0.5 rounded-full font-bold">
            Average: 95.8% ({patientSchedules.length} logs)
          </span>
        </div>

        {/* 7-Day Bar Grid */}
        <div className="grid grid-cols-7 gap-1.5 sm:gap-4 items-end h-40 sm:h-44 pt-4 border-b border-white/10 print:border-neutral-200 pb-4">
          {daysOfWeek.map((day, idx) => {
            const val = mockWeeklyRates[idx];
            return (
              <div key={day} className="flex flex-col items-center h-full justify-end group">
                <span className="text-[10px] sm:text-[11px] font-mono text-sky-300 print:text-neutral-700 font-bold mb-1 opacity-80 sm:opacity-0 group-hover:opacity-100 transition-opacity">
                  {val}%
                </span>
                <div
                  className="w-full max-w-[28px] sm:max-w-[40px] rounded-lg sm:rounded-xl bg-gradient-to-t from-[#0277bd] via-[#2084e4] to-[#4fc3f7] transition-all duration-500 group-hover:brightness-125"
                  style={{ height: `${val}%` }}
                />
                <span className="text-[10px] sm:text-xs font-mono text-neutral-400 print:text-neutral-700 mt-2">
                  {day}
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Clinical Skip Reason Breakdown (Crucial for Doctors!) */}
      <div className="glass-card p-6 sm:p-8 rounded-3xl print:border print:border-neutral-300 space-y-5">
        <div className="flex items-center justify-between border-b border-white/10 print:border-neutral-200 pb-4">
          <div className="flex items-center space-x-2.5">
            <MaterialIcon name="clinical_notes" filled className="text-xl text-sky-400" />
            <div>
              <h3 className="text-base font-bold text-white print:text-black font-display">
                Documented Clinical Skip Reasons
              </h3>
              <p className="text-xs text-neutral-400 print:text-neutral-600 mt-0.5">
                Mandatory reasons recorded when doses are paused by the patient or clinical instructions.
              </p>
            </div>
          </div>
          <span className="text-xs font-mono bg-white/5 border border-white/10 px-2.5 py-1 rounded-xl text-neutral-300">
            Total Paused: 12 doses
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {skipReasons.map((reason) => (
            <div key={reason.label} className="p-4 rounded-2xl bg-white/[0.03] border border-white/5 space-y-2">
              <div className="flex items-center justify-between">
                <div className={`w-8 h-8 rounded-xl ${reason.bg} ${reason.color} flex items-center justify-center`}>
                  <MaterialIcon name={reason.icon} className="text-base" />
                </div>
                <span className={`text-base font-bold font-display ${reason.color}`}>
                  {reason.pct}%
                </span>
              </div>
              <div>
                <span className="text-xs font-bold text-white print:text-black block leading-snug">
                  {reason.label}
                </span>
                <span className="text-[10px] font-mono text-neutral-400 block mt-0.5">
                  {reason.count} recorded instances
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Clinical Metrics Breakdown */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        
        <div className="glass-card p-6 rounded-3xl print:border print:border-neutral-300">
          <span className="text-[11px] font-mono text-neutral-400 uppercase">Intake Timing Precision</span>
          <div className="mt-2 flex items-baseline space-x-2">
            <span className="text-3xl font-extrabold text-white print:text-black font-display">98.2%</span>
            <span className="text-xs text-emerald-400 font-bold">+2.4% vs last week</span>
          </div>
          <p className="text-xs text-neutral-400 print:text-neutral-600 mt-2">
            Doses taken within +/- 30 minutes of prescribed clinical window.
          </p>
        </div>

        <div className="glass-card p-6 rounded-3xl print:border print:border-neutral-300">
          <span className="text-[11px] font-mono text-neutral-400 uppercase">Consecutive Streak</span>
          <div className="mt-2 flex items-baseline space-x-2">
            <span className="text-3xl font-extrabold text-sky-400 font-display">24 Days</span>
          </div>
          <p className="text-xs text-neutral-400 print:text-neutral-600 mt-2">
            Continuous daily adherence without missed critical medications.
          </p>
        </div>

        <div className="glass-card p-6 rounded-3xl print:border print:border-neutral-300">
          <span className="text-[11px] font-mono text-neutral-400 uppercase">Active Prescriptions</span>
          <div className="mt-2 flex items-baseline space-x-2">
            <span className="text-3xl font-extrabold text-sky-400 font-display">{patientMeds.length}</span>
            <span className="text-xs text-neutral-400 font-mono">Regimens</span>
          </div>
          <p className="text-xs text-neutral-400 print:text-neutral-600 mt-2">
            Synchronized directly from the patient’s offline SQLite Room database.
          </p>
        </div>

      </div>

    </div>
  );
};
