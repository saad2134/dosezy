import { useState } from 'react';
import { ScheduleEntry, Medicine } from '../api/types';
import { markDoseTaken, markDoseUndo } from '../api/client';
import { useQueryClient } from '@tanstack/react-query';
import { MaterialIcon } from './MaterialIcon';

interface DoseTimelineProps {
  schedules: ScheduleEntry[];
  medicines: Medicine[];
}

export const DoseTimeline: React.FC<DoseTimelineProps> = ({ schedules, medicines }) => {
  const queryClient = useQueryClient();
  const [loadingId, setLoadingId] = useState<string | null>(null);

  const formatTime = (isoString: string) => {
    try {
      const date = new Date(isoString);
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: true });
    } catch {
      return isoString;
    }
  };

  const handleTake = async (id: string) => {
    setLoadingId(id);
    try {
      await markDoseTaken(id, 'Logged via Caregiver Portal');
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
      queryClient.invalidateQueries({ queryKey: ['patients'] });
    } catch (err) {
      console.error(err);
    } finally {
      setLoadingId(null);
    }
  };

  const handleUndo = async (id: string) => {
    setLoadingId(id);
    try {
      await markDoseUndo(id);
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
      queryClient.invalidateQueries({ queryKey: ['patients'] });
    } catch (err) {
      console.error(err);
    } finally {
      setLoadingId(null);
    }
  };

  return (
    <div className="glass-card p-6 rounded-3xl">
      <div className="flex items-center justify-between mb-6">
        <div>
          <span className="text-[11px] font-mono font-bold text-neutral-400 uppercase tracking-wider">
            Daily Adherence Timeline
          </span>
          <h3 className="text-xl font-bold text-white font-display mt-0.5">
            Today’s Prescription Schedule
          </h3>
        </div>
        <span className="text-xs text-neutral-400 font-mono">
          {schedules.length} Doses Logged
        </span>
      </div>

      {schedules.length === 0 ? (
        <div className="text-center py-12 text-neutral-400 text-xs">
          No scheduled medication doses for today.
        </div>
      ) : (
        <div className="space-y-3">
          {schedules.map((entry) => {
            const med = medicines.find(m => m.id === entry.medicineId);
            const medName = med?.name || 'Medication';
            const medColor = med?.color || '#0284c7';
            const isTaken = entry.status === 'TAKEN';
            const isPending = entry.status === 'PENDING';
            const isMissed = entry.status === 'MISSED';

            return (
              <div
                key={entry.id}
                className="p-4 rounded-2xl bg-white/[0.02] border border-white/5 hover:border-white/10 transition-all flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
              >
                
                {/* Time & Medication Details */}
                <div className="flex items-center space-x-4">
                  
                  {/* Scheduled Time Badge */}
                  <div className="w-20 text-center px-2.5 py-1.5 rounded-xl bg-white/5 border border-white/5 shrink-0">
                    <span className="text-xs font-bold text-white font-mono block">
                      {formatTime(entry.scheduledTime)}
                    </span>
                    <span className="text-[10px] text-neutral-400 font-mono">Scheduled</span>
                  </div>

                  {/* Color Bar Indicator & Names */}
                  <div className="flex items-start space-x-3">
                    <div
                      className="w-1.5 h-10 rounded-full shrink-0 mt-0.5"
                      style={{ backgroundColor: medColor }}
                    />
                    <div>
                      <div className="flex items-center space-x-2">
                        <h4 className="text-sm font-bold text-white">
                          {medName}
                        </h4>
                        <span className="px-2 py-0.5 rounded text-[11px] font-mono font-semibold bg-white/10 text-neutral-300">
                          {entry.dosage} {med?.unit || 'Units'}
                        </span>
                      </div>
                      
                      {med?.instructions && (
                        <p className="text-xs text-neutral-400 mt-0.5">
                          {med.instructions}
                        </p>
                      )}

                      {entry.doseNotes && (
                        <div className="flex items-center space-x-1.5 text-[11px] text-sky-300 mt-1">
                          <MaterialIcon name="description" className="text-sm" />
                          <span>Note: "{entry.doseNotes}"</span>
                        </div>
                      )}
                    </div>
                  </div>

                </div>

                {/* Status Badge & Action Controls */}
                <div className="flex items-center space-x-3 w-full sm:w-auto justify-between sm:justify-end">
                  
                  {/* Status Indicator Pill */}
                  {isTaken && (
                    <span className="px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center space-x-1.5">
                      <MaterialIcon name="check" className="text-sm" />
                      <span>Taken {entry.takenTime ? `(${formatTime(entry.takenTime)})` : ''}</span>
                    </span>
                  )}

                  {isPending && (
                    <span className="px-3 py-1 rounded-full text-xs font-semibold bg-sky-500/10 text-sky-400 border border-sky-500/20 flex items-center space-x-1.5">
                      <MaterialIcon name="schedule" className="text-sm" />
                      <span>Pending Intake</span>
                    </span>
                  )}

                  {isMissed && (
                    <span className="px-3 py-1 rounded-full text-xs font-semibold bg-red-500/10 text-red-400 border border-red-500/20 flex items-center space-x-1.5">
                      <MaterialIcon name="error" className="text-sm" />
                      <span>Missed Dose</span>
                    </span>
                  )}

                  {/* Interactive Button */}
                  {isPending && (
                    <button
                      onClick={() => handleTake(entry.id)}
                      disabled={loadingId === entry.id}
                      className="px-3 py-1.5 rounded-xl text-xs font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 hover:bg-emerald-500/30 active:scale-95 transition-all"
                    >
                      {loadingId === entry.id ? 'Saving...' : 'Mark Taken'}
                    </button>
                  )}

                  {isTaken && (
                    <button
                      onClick={() => handleUndo(entry.id)}
                      disabled={loadingId === entry.id}
                      title="Undo dose"
                      className="p-1.5 rounded-xl text-neutral-400 hover:text-white hover:bg-white/10 active:scale-95 transition-all"
                    >
                      <MaterialIcon name="replay" className="text-base" />
                    </button>
                  )}

                </div>

              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
