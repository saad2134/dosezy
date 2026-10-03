import { useState } from 'react';
import { CaregiverAuth, CaregiverRole } from '../api/types';
import { ThemeSwitcher } from './ThemeSwitcher';

interface AuthScreenProps {
  onAuthenticated: (caregiver: CaregiverAuth) => void;
}

export const AuthScreen: React.FC<AuthScreenProps> = ({ onAuthenticated }) => {
  const [mode, setMode] = useState<'register' | 'login'>('register');
  const [role, setRole] = useState<CaregiverRole>('family');
  const [name, setName] = useState('');
  const [specialty, setSpecialty] = useState('');
  const [clinicName, setClinicName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const endpoint = mode === 'register' ? '/v1/auth/register' : '/v1/auth/login';
      const body = mode === 'register'
        ? { name, email, password, role, specialty, clinicName }
        : { email, password };

      const res = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Authentication failed');
      }

      localStorage.setItem('dosezy_caregiver_id', data.id);
      localStorage.setItem('dosezy_caregiver_name', data.name);
      localStorage.setItem('dosezy_caregiver_email', data.email);
      localStorage.setItem('dosezy_caregiver_role', data.role || 'family');
      if (data.specialty) localStorage.setItem('dosezy_caregiver_specialty', data.specialty);
      if (data.clinicName) localStorage.setItem('dosezy_caregiver_clinic', data.clinicName);
      
      onAuthenticated({
        id: data.id,
        name: data.name,
        email: data.email,
        role: data.role || 'family',
        specialty: data.specialty,
        clinicName: data.clinicName,
      });
    } catch (err: any) {
      setError(err.message || 'Network error occurred');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#030712] flex flex-col items-center justify-center p-4 pt-24 pb-12 relative overflow-x-hidden font-sans">
      
      {/* Top-Right Theme Switcher Button with Dropdown */}
      <div className="fixed top-4 right-4 sm:top-6 sm:right-6 z-30">
        <ThemeSwitcher />
      </div>

      <div className="ambient-glow-blue-primary" />
      <div className="ambient-glow-blue-cyan" />

      <div className="glass-card w-full max-w-md p-6 sm:p-8 rounded-3xl border border-white/10 relative z-10 shadow-2xl my-auto">
        
        {/* Brand Header */}
        <div className="text-center mb-6">
          <div className="w-14 h-14 rounded-2xl p-[2px] bg-gradient-to-tr from-sky-600 via-blue-500 to-cyan-400 mx-auto shadow-[0_0_25px_rgba(2,132,199,0.35)] mb-4">
            <img src="/icon-squircle-1000px.png" alt="Dosezy Icon" className="w-full h-full object-cover rounded-[14px]" />
          </div>
          <h1 className="text-2xl font-extrabold text-white font-display tracking-tight">
            Dosezy Connected Portal
          </h1>
          <p className="text-xs text-neutral-400 mt-1">
            Zero-Knowledge E2EE Family Health & Clinical Audit Platform
          </p>
        </div>

        {/* Tab Switcher (Register vs Sign In) */}
        <div className="grid grid-cols-2 p-1 rounded-2xl bg-white/5 border border-white/10 mb-6 text-xs font-semibold">
          <button
            type="button"
            onClick={() => { setMode('register'); setError(null); }}
            className={`py-2 rounded-xl transition-all ${
              mode === 'register' ? 'bg-sky-600 text-white shadow-md' : 'text-neutral-400 hover:text-white'
            }`}
          >
            Create Account
          </button>
          <button
            type="button"
            onClick={() => { setMode('login'); setError(null); }}
            className={`py-2 rounded-xl transition-all ${
              mode === 'login' ? 'bg-sky-600 text-white shadow-md' : 'text-neutral-400 hover:text-white'
            }`}
          >
            Sign In
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4 text-xs">
          
          {/* Role Selector during Registration */}
          {mode === 'register' && (
            <div>
              <label className="block text-neutral-400 mb-1.5 font-mono uppercase text-[10px]">
                Account Persona & Purpose
              </label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setRole('family')}
                  className={`p-3 rounded-2xl border text-left transition-all flex items-start space-x-2.5 ${
                    role === 'family'
                      ? 'bg-sky-500/15 border-sky-500 text-white shadow-sm ring-1 ring-sky-500/50'
                      : 'bg-white/[0.03] border-white/10 text-neutral-400 hover:text-white hover:bg-white/[0.06]'
                  }`}
                >
                  <span className="text-xl shrink-0">👨‍👩‍👧</span>
                  <div className="min-w-0">
                    <span className="font-bold text-xs block leading-tight text-white">Family Member</span>
                    <span className="text-[10px] text-neutral-400 block mt-0.5 font-mono leading-tight">Daily Alarms & Care</span>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => setRole('doctor')}
                  className={`p-3 rounded-2xl border text-left transition-all flex items-start space-x-2.5 ${
                    role === 'doctor'
                      ? 'bg-sky-500/15 border-sky-500 text-white shadow-sm ring-1 ring-sky-500/50'
                      : 'bg-white/[0.03] border-white/10 text-neutral-400 hover:text-white hover:bg-white/[0.06]'
                  }`}
                >
                  <span className="text-xl shrink-0">🩺</span>
                  <div className="min-w-0">
                    <span className="font-bold text-xs block leading-tight text-white">Healthcare Doctor</span>
                    <span className="text-[10px] text-neutral-400 block mt-0.5 font-mono leading-tight">Clinical Audits & PDF</span>
                  </div>
                </button>
              </div>
            </div>
          )}

          {/* Registration Fields */}
          {mode === 'register' && role === 'family' && (
            <div>
              <label className="block text-neutral-400 mb-1.5 font-mono uppercase text-[10px]">
                Full Name / Relationship
              </label>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Robert Vance (Son)"
                className="w-full px-4 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white placeholder-neutral-500 focus:outline-none focus:border-sky-500 transition-all text-sm sm:text-xs"
              />
            </div>
          )}

          {mode === 'register' && role === 'doctor' && (
            <div className="space-y-3">
              <div>
                <label className="block text-neutral-400 mb-1.5 font-mono uppercase text-[10px]">
                  Physician / Clinician Full Name
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Dr. Sarah Jenkins, MD"
                  className="w-full px-4 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white placeholder-neutral-500 focus:outline-none focus:border-sky-500 transition-all text-sm sm:text-xs"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-neutral-400 mb-1.5 font-mono uppercase text-[10px]">
                    Clinical Specialty
                  </label>
                  <input
                    type="text"
                    value={specialty}
                    onChange={(e) => setSpecialty(e.target.value)}
                    placeholder="e.g. Geriatrics"
                    className="w-full px-3 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white placeholder-neutral-500 focus:outline-none focus:border-sky-500 transition-all text-sm sm:text-xs"
                  />
                </div>

                <div>
                  <label className="block text-neutral-400 mb-1.5 font-mono uppercase text-[10px]">
                    Clinic / Hospital
                  </label>
                  <input
                    type="text"
                    value={clinicName}
                    onChange={(e) => setClinicName(e.target.value)}
                    placeholder="e.g. Mercy Health"
                    className="w-full px-3 py-2 rounded-xl bg-white/[0.04] border border-white/10 text-white placeholder-neutral-500 focus:outline-none focus:border-sky-500 transition-all text-sm sm:text-xs"
                  />
                </div>
              </div>
            </div>
          )}

          <div>
            <label className="block text-neutral-400 mb-1.5 font-mono uppercase text-[10px]">
              Email Address
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder={role === 'doctor' && mode === 'register' ? 'doctor@clinic.org' : 'caregiver@family.org'}
              className="w-full px-4 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white placeholder-neutral-500 focus:outline-none focus:border-sky-500 transition-all text-sm sm:text-xs"
            />
          </div>

          <div>
            <label className="block text-neutral-400 mb-1.5 font-mono uppercase text-[10px]">
              Password
            </label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••••••"
              className="w-full px-4 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-white placeholder-neutral-500 focus:outline-none focus:border-sky-500 transition-all text-sm sm:text-xs"
            />
          </div>

          {error && (
            <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-300 text-xs">
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 rounded-xl bg-gradient-to-r from-[#0277bd] via-[#2084e4] to-[#0284c7] hover:from-[#01579b] hover:via-[#0277bd] hover:to-[#2084e4] text-white font-bold text-xs shadow-lg hover:shadow-sky-500/25 transition-all mt-2 cursor-pointer"
          >
            {loading
              ? 'Processing...'
              : mode === 'register'
              ? (role === 'doctor' ? 'Register Healthcare Doctor Account' : 'Register Family Caregiver Account')
              : 'Sign In to Portal'}
          </button>
        </form>

      </div>
    </div>
  );
};
