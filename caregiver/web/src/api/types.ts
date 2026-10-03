export interface CaregiverPatientSummary {
  userId: string;
  fullName: string;
  adherenceRate: number;
  activeMedicinesCount: number;
  missedDosesCount: number;
  todayPendingCount: number;
  todayTakenCount: number;
  todayLateCount: number;
  allergies?: string | null;
  chronicConditions?: string | null;
  emergencyContactName?: string | null;
  emergencyContactPhone?: string | null;
  lastSyncedAt?: string | null;
}

export interface Medicine {
  id: string;
  userId: string;
  name: string;
  dosage: number;
  unit: string;
  frequency: string;
  instructions?: string | null;
  color: string;
  iconName: string;
  notes?: string | null;
  stockCount?: number | null;
  refillReminderThreshold?: number | null;
  scheduledTimes: string[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ScheduleEntry {
  id: string;
  medicineId: string;
  userId: string;
  scheduledTime: string;
  dosage: number;
  status: 'PENDING' | 'TAKEN' | 'SKIPPED' | 'MISSED';
  doseNotes?: string | null;
  takenTime?: string | null;
  createdAt: string;
  updatedAt: string;
  medicineName?: string;
  medicineColor?: string;
  medicineUnit?: string;
}

export interface PairingResponse {
  pairingCode: string;
  status: 'PENDING' | 'CONFIRMED' | 'EXPIRED';
  patientId?: string;
  message?: string;
}

export type CaregiverRole = 'family' | 'doctor';

export interface CaregiverAuth {
  id: string;
  email: string;
  name: string;
  role: CaregiverRole;
  specialty?: string | null;
  clinicName?: string | null;
}
