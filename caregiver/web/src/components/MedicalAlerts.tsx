import { CaregiverPatientSummary, Medicine } from '../api/types';
import { MaterialIcon } from './MaterialIcon';

interface MedicalAlertsProps {
  patient: CaregiverPatientSummary;
  medicines: Medicine[];
}

export const MedicalAlerts: React.FC<MedicalAlertsProps> = ({ patient, medicines }) => {
  const lowStockMeds = medicines.filter(
    m => m.stockCount !== null && m.stockCount !== undefined && m.stockCount <= (m.refillReminderThreshold || 5)
  );

  return (
    <div className="space-y-6">
      
      {/* Emergency & Clinical Safety Card */}
      <div className="glass-card p-6 rounded-3xl border border-red-500/20">
        <div className="flex items-center space-x-2 text-red-400 mb-4">
          <MaterialIcon name="health_and_safety" filled className="text-xl" />
          <h3 className="text-base font-bold text-white font-display">
            Emergency & Safety Profile
          </h3>
        </div>

        <div className="space-y-4 text-xs">
          
          {/* Allergies */}
          <div>
            <span className="text-neutral-400 font-mono uppercase block text-[10px]">
              Known Drug Allergies
            </span>
            <p className="text-red-300 font-semibold mt-0.5">
              {patient.allergies || 'No documented drug allergies'}
            </p>
          </div>

          {/* Chronic Conditions */}
          <div>
            <span className="text-neutral-400 font-mono uppercase block text-[10px]">
              Chronic Medical Conditions
            </span>
            <p className="text-neutral-200 mt-0.5">
              {patient.chronicConditions || 'None documented'}
            </p>
          </div>

          {/* Emergency Contact */}
          {patient.emergencyContactName && (
            <div className="pt-3 border-t border-white/5 flex items-center justify-between">
              <div>
                <span className="text-neutral-400 font-mono uppercase block text-[10px]">
                  Emergency Contact
                </span>
                <span className="text-white font-semibold block mt-0.5">
                  {patient.emergencyContactName}
                </span>
              </div>

              {patient.emergencyContactPhone && (
                <a
                  href={`tel:${patient.emergencyContactPhone}`}
                  className="px-3 py-1.5 rounded-xl bg-red-500/20 text-red-300 border border-red-500/30 hover:bg-red-500/30 text-xs font-bold flex items-center space-x-1.5 transition-all"
                >
                  <MaterialIcon name="call" className="text-sm" />
                  <span>Call</span>
                </a>
              )}
            </div>
          )}

        </div>
      </div>

      {/* Refill / Stock Warnings */}
      {lowStockMeds.length > 0 && (
        <div className="glass-card p-6 rounded-3xl border border-amber-500/20">
          <div className="flex items-center space-x-2 text-amber-400 mb-3">
            <MaterialIcon name="inventory_2" filled className="text-lg" />
            <h3 className="text-base font-bold text-white font-display">
              Prescription Refill Alerts
            </h3>
          </div>

          <div className="space-y-2.5">
            {lowStockMeds.map(m => (
              <div key={m.id} className="p-3 rounded-xl bg-white/[0.02] border border-amber-500/10 flex items-center justify-between text-xs">
                <div>
                  <span className="font-bold text-white block">{m.name}</span>
                  <span className="text-neutral-400 text-[11px] font-mono">
                    Threshold: {m.refillReminderThreshold} units
                  </span>
                </div>
                <span className="px-2.5 py-1 rounded-full text-xs font-mono font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30">
                  {m.stockCount} Left
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

    </div>
  );
};
