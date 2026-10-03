import { CaregiverPatientSummary } from '../api/types';
import { MaterialIcon } from './MaterialIcon';

interface AdherenceCardProps {
  patient: CaregiverPatientSummary;
}

export const AdherenceCard: React.FC<AdherenceCardProps> = ({ patient }) => {
  const rate = patient.adherenceRate || 0;
  const radius = 38;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (rate / 100) * circumference;

  let scoreColor = '#10b981'; // Green
  let statusText = 'Excellent Adherence';
  if (rate < 75) {
    scoreColor = '#ef4444'; // Red
    statusText = 'Attention Required';
  } else if (rate < 85) {
    scoreColor = '#f59e0b'; // Amber
    statusText = 'Moderate Compliance';
  }

  return (
    <div className="glass-card p-4 sm:p-6 rounded-3xl relative overflow-hidden">
      
      {/* Top Header */}
      <div className="flex flex-wrap items-center justify-between gap-2 mb-6">
        <div>
          <span className="text-[10px] sm:text-[11px] font-mono font-bold text-neutral-400 uppercase tracking-wider">
            Patient Compliance Score
          </span>
          <h2 className="text-lg sm:text-xl font-bold text-white font-display mt-0.5">
            {patient.fullName}
          </h2>
        </div>
        
        <div className="flex items-center space-x-1.5 px-3 py-1 rounded-full bg-sky-500/10 border border-sky-500/20 text-sky-300 text-xs font-semibold">
          <MaterialIcon name="local_fire_department" filled className="text-sm text-sky-400" />
          <span>7-Day Streak</span>
        </div>
      </div>

      {/* Center Grid: Circular Gauge & Metric Pills */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-6 items-center">
        
        {/* SVG Circular Progress Gauge (Col Span 2) */}
        <div className="md:col-span-2 flex flex-col sm:flex-row items-center sm:items-start text-center sm:text-left gap-4 sm:space-x-5">
          <div className="relative w-24 h-24 sm:w-28 sm:h-28 flex items-center justify-center shrink-0">
            <svg className="w-full h-full transform -rotate-90" viewBox="0 0 96 96">
              <circle
                cx="48"
                cy="48"
                r={radius}
                stroke="currentColor"
                strokeWidth="8"
                className="text-white/10"
                fill="transparent"
              />
              <circle
                cx="48"
                cy="48"
                r={radius}
                stroke={scoreColor}
                strokeWidth="8"
                fill="transparent"
                strokeDasharray={circumference}
                strokeDashoffset={strokeDashoffset}
                strokeLinecap="round"
                className="transition-all duration-1000 ease-out"
              />
            </svg>
            <div className="absolute flex flex-col items-center justify-center">
              <span className="text-2xl font-extrabold text-white font-display tracking-tight">
                {rate}%
              </span>
              <span className="text-[10px] font-mono text-neutral-400 uppercase">
                Adherence
              </span>
            </div>
          </div>

          <div>
            <span
              className="inline-block text-xs font-bold px-2.5 py-0.5 rounded-full mb-1.5"
              style={{ backgroundColor: `${scoreColor}20`, color: scoreColor }}
            >
              {statusText}
            </span>
            <p className="text-xs text-neutral-400 leading-relaxed">
              Based on scheduled prescription doses taken within prescribed time windows.
            </p>
          </div>
        </div>

        {/* 4 Metric Tiles (Col Span 3) */}
        <div className="md:col-span-3 grid grid-cols-2 sm:grid-cols-4 gap-3">
          
          {/* Taken On Time */}
          <div className="p-3.5 rounded-2xl bg-white/[0.03] border border-white/5 flex flex-col justify-between">
            <div className="flex items-center justify-between text-emerald-400 mb-2">
              <span className="text-xs font-medium">Taken</span>
              <MaterialIcon name="check_circle" filled className="text-base" />
            </div>
            <div>
              <span className="text-2xl font-extrabold text-white font-display">
                {patient.todayTakenCount || 0}
              </span>
              <span className="text-[10px] block text-neutral-400 font-mono mt-0.5">On Schedule</span>
            </div>
          </div>

          {/* Taken Late */}
          <div className="p-3.5 rounded-2xl bg-white/[0.03] border border-white/5 flex flex-col justify-between">
            <div className="flex items-center justify-between text-amber-400 mb-2">
              <span className="text-xs font-medium">Late</span>
              <MaterialIcon name="schedule" filled className="text-base" />
            </div>
            <div>
              <span className="text-2xl font-extrabold text-white font-display">
                {patient.todayLateCount || 0}
              </span>
              <span className="text-[10px] block text-neutral-400 font-mono mt-0.5">Past Window</span>
            </div>
          </div>

          {/* Pending */}
          <div className="p-3.5 rounded-2xl bg-white/[0.03] border border-white/5 flex flex-col justify-between">
            <div className="flex items-center justify-between text-sky-400 mb-2">
              <span className="text-xs font-medium">Pending</span>
              <MaterialIcon name="alarm" filled className="text-base" />
            </div>
            <div>
              <span className="text-2xl font-extrabold text-white font-display">
                {patient.todayPendingCount || 0}
              </span>
              <span className="text-[10px] block text-neutral-400 font-mono mt-0.5">Due Today</span>
            </div>
          </div>

          {/* Missed */}
          <div className="p-3.5 rounded-2xl bg-white/[0.03] border border-white/5 flex flex-col justify-between">
            <div className="flex items-center justify-between text-red-400 mb-2">
              <span className="text-xs font-medium">Missed</span>
              <MaterialIcon name="cancel" filled className="text-base" />
            </div>
            <div>
              <span className="text-2xl font-extrabold text-white font-display">
                {patient.missedDosesCount || 0}
              </span>
              <span className="text-[10px] block text-neutral-400 font-mono mt-0.5">Unconfirmed</span>
            </div>
          </div>

        </div>

      </div>

    </div>
  );
};
