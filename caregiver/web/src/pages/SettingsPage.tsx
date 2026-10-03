import { useEffect, useState } from 'react';
import { fetchHealth } from '../api/client';
import { MaterialIcon } from '../components/MaterialIcon';

interface SettingsPageProps {
  isConnected: boolean;
}

export const SettingsPage: React.FC<SettingsPageProps> = ({ isConnected }) => {
  const [healthData, setHealthData] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  const loadHealth = async () => {
    setLoading(true);
    try {
      const data = await fetchHealth();
      setHealthData(data);
    } catch (err) {
      setHealthData({ status: 'unreachable' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadHealth();
  }, []);

  return (
    <div className="space-y-8 max-w-4xl">
      <div>
        <span className="text-[11px] font-mono font-bold text-neutral-400 uppercase tracking-wider">
          Platform Architecture & Diagnostics
        </span>
        <h2 className="text-2xl font-bold text-white font-display mt-0.5">
          Server & Infrastructure Status
        </h2>
      </div>

      {/* Server Health Diagnostics Card */}
      <div className="glass-card p-6 sm:p-8 rounded-3xl space-y-6">
        <div className="flex items-center justify-between pb-4 border-b border-white/10">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-sky-500/10 border border-sky-500/20 text-sky-400 flex items-center justify-center">
              <MaterialIcon name="dns" filled className="text-2xl" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white font-display">
                Dosezy Backend Node
              </h3>
              <span className="text-xs font-mono text-neutral-400">
                Fastify 5 • SQLite / PostgreSQL Engine
              </span>
            </div>
          </div>

          <button
            onClick={loadHealth}
            disabled={loading}
            className="p-2 rounded-xl text-neutral-400 hover:text-white hover:bg-white/10 transition-all"
            title="Refresh Diagnostics"
          >
            <MaterialIcon name="refresh" className={`text-xl ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs font-mono">
          <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/5">
            <span className="text-neutral-400 block text-[10px] uppercase">Service State</span>
            <span className="text-sm font-bold text-emerald-400 mt-1 flex items-center space-x-1.5">
              <MaterialIcon name="check_circle" filled className="text-base" />
              <span>{healthData?.status === 'ok' ? 'HEALTHY & OPERATIONAL' : 'OFFLINE'}</span>
            </span>
          </div>

          <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/5">
            <span className="text-neutral-400 block text-[10px] uppercase">API Version</span>
            <span className="text-sm font-bold text-white mt-1 block">
              v{healthData?.version || '1.0.0'}
            </span>
          </div>

          <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/5">
            <span className="text-neutral-400 block text-[10px] uppercase">Live SSE Stream</span>
            <span className={`text-sm font-bold mt-1 block ${isConnected ? 'text-emerald-400' : 'text-amber-400'}`}>
              {isConnected ? 'STREAMING ACTIVE' : 'DISCONNECTED'}
            </span>
          </div>
        </div>
      </div>

      {/* Security & Cryptography Verification */}
      <div className="glass-card p-6 sm:p-8 rounded-3xl space-y-4">
        <div className="flex items-center space-x-3 text-sky-400 mb-2">
          <MaterialIcon name="security" filled className="text-2xl" />
          <h3 className="text-base font-bold text-white font-display">
            Zero-Knowledge E2EE Encryption
          </h3>
        </div>

        <p className="text-xs text-neutral-300 leading-relaxed">
          In strict compliance with Dosezy's local-first privacy architecture, medication schedules and patient intake logs are encrypted prior to network transmission using <strong>AES-256-GCM</strong>. Only authorized paired devices holding the shared cryptographic pairing secret can decrypt patient data.
        </p>

        <div className="pt-2 flex items-center space-x-2 text-xs font-mono text-sky-300">
          <span>✓ Zero Third-Party SSO Trackers</span>
          <span>•</span>
          <span>✓ Local Hardware SQLite DB</span>
          <span>•</span>
          <span>✓ MIT Licensed</span>
        </div>
      </div>

    </div>
  );
};
