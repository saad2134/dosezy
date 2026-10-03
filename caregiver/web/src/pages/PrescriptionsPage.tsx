import { Medicine, CaregiverPatientSummary } from '../api/types';
import { MaterialIcon } from '../components/MaterialIcon';

interface PrescriptionsPageProps {
  patient: CaregiverPatientSummary | null;
  medicines: Medicine[];
}

export const PrescriptionsPage: React.FC<PrescriptionsPageProps> = ({ patient, medicines }) => {
  if (!patient) {
    return <div className="py-20 text-center text-neutral-400 text-xs">No patient selected.</div>;
  }

  const patientMeds = medicines.filter(m => m.userId === patient.userId);

  return (
    <div className="space-y-6">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <span className="text-[11px] font-mono font-bold text-neutral-400 uppercase tracking-wider">
            Active Prescriptions Roster
          </span>
          <h2 className="text-2xl font-bold text-white font-display mt-0.5">
            Medications for {patient.fullName}
          </h2>
        </div>

        <span className="px-3 py-1 rounded-full text-xs font-mono font-bold bg-sky-500/10 text-sky-300 border border-sky-500/20">
          {patientMeds.length} Active Prescriptions
        </span>
      </div>

      {/* Medication Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {patientMeds.map((med) => {
          const isLowStock = med.stockCount !== null && med.stockCount !== undefined && med.stockCount <= (med.refillReminderThreshold || 5);

          return (
            <div
              key={med.id}
              className="glass-card p-5 rounded-3xl space-y-4 hover:border-sky-500/30 transition-all"
            >
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div className="flex items-center space-x-3">
                  <div
                    className="w-11 h-11 rounded-2xl flex items-center justify-center text-white text-xl shadow-md"
                    style={{ backgroundColor: med.color }}
                  >
                    <MaterialIcon name="medication" filled className="text-2xl" />
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-white">
                      {med.name}
                    </h3>
                    <span className="text-xs text-neutral-400 font-mono">
                      {med.dosage} {med.unit} • {med.frequency}
                    </span>
                  </div>
                </div>

                {isLowStock && (
                  <span className="px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30">
                    Refill Soon ({med.stockCount} left)
                  </span>
                )}
              </div>

              {med.instructions && (
                <div className="p-3 rounded-2xl bg-white/[0.03] border border-white/5 text-xs text-neutral-300 leading-relaxed">
                  <span className="text-neutral-500 block text-[10px] font-mono uppercase mb-0.5">Directions</span>
                  {med.instructions}
                </div>
              )}

              <div className="pt-2 border-t border-white/5 flex items-center justify-between text-xs text-neutral-400">
                <div className="flex items-center space-x-1.5">
                  <MaterialIcon name="schedule" className="text-sm text-sky-400" />
                  <span className="font-mono">{med.scheduledTimes.join(', ')}</span>
                </div>

                {med.stockCount !== null && (
                  <span className="font-mono text-[11px]">
                    Remaining Stock: <strong className="text-white">{med.stockCount}</strong>
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </div>

    </div>
  );
};
