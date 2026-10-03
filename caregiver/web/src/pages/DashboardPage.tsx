import { CaregiverPatientSummary, Medicine, ScheduleEntry } from '../api/types';
import { AdherenceCard } from '../components/AdherenceCard';
import { DoseTimeline } from '../components/DoseTimeline';
import { MedicalAlerts } from '../components/MedicalAlerts';
import { MaterialIcon } from '../components/MaterialIcon';

interface DashboardPageProps {
  patient: CaregiverPatientSummary | null;
  medicines: Medicine[];
  schedules: ScheduleEntry[];
  onOpenPairModal: () => void;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({
  patient,
  medicines,
  schedules,
  onOpenPairModal,
}) => {
  if (!patient) {
    return (
      <div className="py-20 text-center max-w-md mx-auto">
        <div className="w-16 h-16 rounded-3xl bg-sky-500/10 border border-sky-500/20 text-sky-400 flex items-center justify-center text-3xl mx-auto mb-4">
          <MaterialIcon name="medical_services" filled className="text-3xl text-sky-400" />
        </div>
        <h2 className="text-2xl font-bold text-white font-display">No Patients Linked</h2>
        <p className="text-xs text-neutral-400 mt-2 leading-relaxed">
          Link a family member’s Dosezy app using their 6-digit pairing code to begin monitoring medication adherence in real time.
        </p>
        <button
          onClick={onOpenPairModal}
          className="mt-6 px-6 py-3 rounded-2xl bg-gradient-to-r from-[#0277bd] via-[#2084e4] to-[#0284c7] hover:from-[#01579b] text-white font-bold text-xs shadow-lg hover:shadow-sky-500/20 transition-all inline-flex items-center space-x-2"
        >
          <MaterialIcon name="person_add" className="text-base" />
          <span>Pair First Patient</span>
        </button>
      </div>
    );
  }

  const patientMeds = medicines.filter(m => m.userId === patient.userId);
  const patientSchedules = schedules.filter(s => s.userId === patient.userId);

  return (
    <div className="space-y-8">
      {/* 1. Top Compliance Gauge Card */}
      <AdherenceCard patient={patient} />

      {/* 2. Main 2-Column Layout: Timeline (Left) + Alerts & Prescriptions (Right) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        
        {/* Left Column: Today's Dose Schedule Timeline */}
        <div className="lg:col-span-8 space-y-6">
          <DoseTimeline schedules={patientSchedules} medicines={patientMeds} />
        </div>

        {/* Right Column: Emergency & Prescriptions Roster */}
        <div className="lg:col-span-4 space-y-6">
          <MedicalAlerts patient={patient} medicines={patientMeds} />

          {/* Active Prescriptions Roster Card */}
          <div className="glass-card p-6 rounded-3xl">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center space-x-2">
                <MaterialIcon name="medication" filled className="text-sky-400 text-lg" />
                <h3 className="text-base font-bold text-white font-display">
                  Active Prescriptions
                </h3>
              </div>
              <span className="text-xs font-mono text-neutral-400">
                {patientMeds.length} Active
              </span>
            </div>

            <div className="space-y-3">
              {patientMeds.map(m => (
                <div key={m.id} className="p-3 rounded-2xl bg-white/[0.02] border border-white/5 flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <div
                      className="w-2.5 h-8 rounded-full"
                      style={{ backgroundColor: m.color }}
                    />
                    <div>
                      <h4 className="text-xs font-bold text-white">{m.name}</h4>
                      <span className="text-[11px] text-neutral-400 font-mono">
                        {m.dosage} {m.unit} • {m.frequency}
                      </span>
                    </div>
                  </div>

                  <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-white/5 text-neutral-300">
                    {m.scheduledTimes.join(', ')}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>

      </div>
    </div>
  );
};
