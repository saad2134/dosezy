import { CaregiverPatientSummary, Medicine } from '../api/types';
import { MaterialIcon } from '../components/MaterialIcon';

interface SafetyProfilePageProps {
  patient: CaregiverPatientSummary | null;
  medicines: Medicine[];
}

export const SafetyProfilePage: React.FC<SafetyProfilePageProps> = ({ patient, medicines }) => {
  if (!patient) {
    return <div className="py-20 text-center text-neutral-400 text-xs">No patient selected.</div>;
  }

  const patientMeds = medicines.filter(m => m.userId === patient.userId);
  const lowStockMeds = patientMeds.filter(
    m => m.stockCount !== null && m.stockCount !== undefined && m.stockCount <= (m.refillReminderThreshold || 5)
  );

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <span className="text-[11px] font-mono font-bold text-neutral-400 uppercase tracking-wider">
          Clinical Safety & Emergency
        </span>
        <h2 className="text-2xl font-bold text-white font-display mt-0.5">
          Emergency Profile for {patient.fullName}
        </h2>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        
        {/* Drug Allergies */}
        <div className="glass-card p-6 rounded-3xl border border-red-500/20 space-y-3">
          <div className="flex items-center space-x-2 text-red-400">
            <MaterialIcon name="warning" filled className="text-xl" />
            <h3 className="text-base font-bold text-white font-display">
              Known Drug Allergies
            </h3>
          </div>
          <div className="p-4 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-200 text-sm font-semibold">
            {patient.allergies || 'No known allergies documented.'}
          </div>
          <p className="text-xs text-neutral-400">
            Emergency responders and clinicians should verify prior to administering acute treatments.
          </p>
        </div>

        {/* Chronic Conditions */}
        <div className="glass-card p-6 rounded-3xl border border-white/10 space-y-3">
          <div className="flex items-center space-x-2 text-sky-400">
            <MaterialIcon name="health_and_safety" filled className="text-xl" />
            <h3 className="text-base font-bold text-white font-display">
              Chronic Medical Conditions
            </h3>
          </div>
          <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/5 text-neutral-200 text-sm font-medium">
            {patient.chronicConditions || 'No conditions documented.'}
          </div>
          <p className="text-xs text-neutral-400">
            Logged directly from the patient’s offline Dosezy medical profile.
          </p>
        </div>

        {/* Emergency Contact */}
        <div className="glass-card p-6 rounded-3xl border border-white/10 space-y-3">
          <div className="flex items-center space-x-2 text-sky-400">
            <MaterialIcon name="contact_phone" filled className="text-xl" />
            <h3 className="text-base font-bold text-white font-display">
              Designated Emergency Contact
            </h3>
          </div>

          <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/5 flex items-center justify-between">
            <div>
              <span className="text-xs text-neutral-400 block font-mono">Contact Name</span>
              <span className="text-base font-bold text-white mt-0.5 block">
                {patient.emergencyContactName || 'None assigned'}
              </span>
            </div>

            {patient.emergencyContactPhone && (
              <a
                href={`tel:${patient.emergencyContactPhone}`}
                className="px-4 py-2 rounded-xl bg-sky-500/20 text-sky-300 border border-sky-500/30 hover:bg-sky-500/30 text-xs font-bold flex items-center space-x-2 transition-all"
              >
                <MaterialIcon name="call" className="text-sm" />
                <span>Call ({patient.emergencyContactPhone})</span>
              </a>
            )}
          </div>
        </div>

        {/* Refill Thresholds */}
        <div className="glass-card p-6 rounded-3xl border border-amber-500/20 space-y-3">
          <div className="flex items-center space-x-2 text-amber-400">
            <MaterialIcon name="inventory_2" filled className="text-xl" />
            <h3 className="text-base font-bold text-white font-display">
              Prescription Refill Deficits
            </h3>
          </div>

          {lowStockMeds.length === 0 ? (
            <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-xs">
              All prescription inventory levels are currently above danger thresholds.
            </div>
          ) : (
            <div className="space-y-2">
              {lowStockMeds.map(m => (
                <div key={m.id} className="p-3 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-between text-xs">
                  <span className="font-bold text-white">{m.name}</span>
                  <span className="font-mono text-amber-300 font-bold">{m.stockCount} units remaining</span>
                </div>
              ))}
            </div>
          )}
        </div>

      </div>
    </div>
  );
};
