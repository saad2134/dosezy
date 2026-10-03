import { CaregiverPatientSummary, Medicine, ScheduleEntry, PairingResponse } from './types';

const API_BASE = '';

export async function fetchHealth() {
  const res = await fetch(`${API_BASE}/health`);
  if (!res.ok) throw new Error('Server unreachable');
  return res.json();
}

export async function fetchPatients(): Promise<CaregiverPatientSummary[]> {
  const res = await fetch(`${API_BASE}/v1/caregiver/patients`);
  if (!res.ok) throw new Error('Failed to load patient roster');
  return res.json();
}

export async function fetchMedicines(): Promise<Medicine[]> {
  const res = await fetch(`${API_BASE}/v1/medicines`);
  if (!res.ok) throw new Error('Failed to load prescriptions');
  return res.json();
}

export async function fetchSchedules(): Promise<ScheduleEntry[]> {
  const res = await fetch(`${API_BASE}/v1/schedules`);
  if (!res.ok) throw new Error('Failed to load schedule timeline');
  return res.json();
}

export async function redeemPairingCode(code: string): Promise<PairingResponse> {
  const res = await fetch(`${API_BASE}/v1/caregiver/pair`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ pairingCode: code }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Failed to redeem pairing code');
  }
  return res.json();
}

export async function markDoseTaken(scheduleId: string, doseNotes?: string): Promise<ScheduleEntry> {
  const res = await fetch(`${API_BASE}/v1/schedules/${scheduleId}/take`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ doseNotes }),
  });
  if (!res.ok) throw new Error('Failed to mark dose taken');
  return res.json();
}

export async function markDoseUndo(scheduleId: string): Promise<ScheduleEntry> {
  const res = await fetch(`${API_BASE}/v1/schedules/${scheduleId}/undo`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error('Failed to undo dose');
  return res.json();
}
