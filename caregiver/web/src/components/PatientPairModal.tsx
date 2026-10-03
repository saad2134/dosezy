import { useState } from 'react';
import { redeemPairingCode } from '../api/client';
import { useQueryClient } from '@tanstack/react-query';
import { MaterialIcon } from './MaterialIcon';
import CodeSlots from './CodeSlots';

interface PatientPairModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const PatientPairModal: React.FC<PatientPairModalProps> = ({ isOpen, onClose }) => {
  const queryClient = useQueryClient();
  const [code, setCode] = useState('');
  const [status, setStatus] = useState<'idle' | 'error' | 'success'>('idle');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleVerify = async (enteredCode: string) => {
    const cleanCode = enteredCode.trim().toUpperCase();
    if (cleanCode.length !== 6) {
      setError('Please enter a valid 6-character pairing code.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await redeemPairingCode(cleanCode);
      setStatus('success');
      queryClient.invalidateQueries({ queryKey: ['patients'] });
      setTimeout(() => {
        setStatus('idle');
        setCode('');
        onClose();
      }, 1600);
    } catch (err: any) {
      setStatus('error');
      setError(err.message || 'Failed to redeem pairing code. Verify it has not expired.');
    } finally {
      setLoading(false);
    }
  };

  const handleManualSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (code.length === 6) {
      handleVerify(code);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md">
      <div className="glass-card w-full max-w-md p-6 sm:p-8 rounded-3xl border border-white/15 relative shadow-2xl animate-in fade-in zoom-in-95 duration-200">
        
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-neutral-400 hover:text-white hover:bg-white/10 transition-all"
        >
          <MaterialIcon name="close" className="text-xl" />
        </button>

        {/* Header */}
        <div className="flex items-center space-x-3 mb-6">
          <div className="w-12 h-12 rounded-2xl bg-sky-500/10 border border-sky-500/20 flex items-center justify-center text-sky-400 text-xl font-bold">
            <MaterialIcon name="key" filled className="text-2xl" />
          </div>
          <div>
            <h3 className="text-xl font-bold text-white font-display">
              Pair Monitored Patient
            </h3>
            <span className="text-xs text-neutral-400">
              Zero-knowledge E2EE pairing handshake
            </span>
          </div>
        </div>

        {/* Steps Guide */}
        <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/5 space-y-2 text-xs text-neutral-300 mb-6">
          <div className="flex items-start space-x-2">
            <span className="text-sky-400 font-mono font-bold">1.</span>
            <span>On the patient’s phone, open Dosezy → <strong>Menu → Caregiver Setup</strong>.</span>
          </div>
          <div className="flex items-start space-x-2">
            <span className="text-sky-400 font-mono font-bold">2.</span>
            <span>Tap <strong>"Connect a Caregiver"</strong> to generate a temporary 6-digit code.</span>
          </div>
          <div className="flex items-start space-x-2">
            <span className="text-sky-400 font-mono font-bold">3.</span>
            <span>Enter the code below to link this web dashboard without third-party SSO.</span>
          </div>
        </div>

        {/* Form with React Bits CodeSlots */}
        <form onSubmit={handleManualSubmit} className="space-y-5">
          <div className="flex flex-col items-center justify-center">
            <label className="block text-xs font-mono text-neutral-400 mb-3 uppercase tracking-wider text-center">
              6-Character Pairing Code
            </label>
            
            <div className="flex justify-center my-1 w-full overflow-x-auto py-1">
              <CodeSlots
                length={6}
                value={code}
                status={status}
                autoFocus
                onChange={(c) => {
                  setCode(c);
                  if (status !== 'idle') setStatus('idle');
                  if (error) setError(null);
                }}
                onComplete={(completedCode) => {
                  handleVerify(completedCode);
                }}
                accentColor="#0284c7"
                inkColor="#38bdf8"
                slotColor="#0f172a"
                digitColor="#ffffff"
                dangerColor="#ef4444"
                slotSize={44}
                gap={8}
                radius={12}
                bounce={0.2}
                settle={0.3}
                rise={8}
                cascade={20}
              />
            </div>
          </div>

          {error && (
            <div className="p-3 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-300 text-xs text-center flex items-center justify-center space-x-2 animate-in fade-in duration-200">
              <MaterialIcon name="error" className="text-base text-red-400" />
              <span>{error}</span>
            </div>
          )}

          {status === 'success' && (
            <div className="p-3 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-xs flex items-center justify-center space-x-2 animate-in fade-in duration-200">
              <MaterialIcon name="verified" filled className="text-base text-emerald-400" />
              <span>Patient paired successfully! Loading data...</span>
            </div>
          )}

          <div className="pt-2">
            <button
              type="submit"
              disabled={loading || code.length !== 6 || status === 'success'}
              className="w-full py-3.5 rounded-2xl bg-gradient-to-r from-[#0277bd] via-[#2084e4] to-[#0284c7] hover:from-[#01579b] hover:via-[#0277bd] hover:to-[#2084e4] text-white font-bold text-sm shadow-lg hover:shadow-sky-500/25 disabled:opacity-50 disabled:cursor-not-allowed transition-all flex items-center justify-center space-x-2"
            >
              {loading ? (
                <span>Verifying Code...</span>
              ) : status === 'success' ? (
                <span>Pairing Confirmed!</span>
              ) : (
                <span>Confirm & Link Patient</span>
              )}
            </button>
          </div>
        </form>

      </div>
    </div>
  );
};
