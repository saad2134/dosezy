export type Gender = 'MALE' | 'FEMALE' | 'DO_NOT_SPECIFY';
export type Theme = 'LIGHT' | 'DARK' | 'SYSTEM';
export type TimeFormat = 'HOUR_12' | 'HOUR_24';
export type Language =
  | 'SYSTEM'
  | 'ENGLISH'
  | 'SPANISH'
  | 'HINDI'
  | 'CHINESE'
  | 'PORTUGUESE'
  | 'ARABIC'
  | 'FRENCH'
  | 'GERMAN'
  | 'JAPANESE'
  | 'RUSSIAN'
  | 'ITALIAN'
  | 'BENGALI';

export type DosageUnit = 'MG' | 'MCG' | 'ML' | 'DROP' | 'TABLET' | 'CAPSULE';
export type FrequencyPattern =
  | 'DAILY'
  | 'WEEKLY'
  | 'MONTHLY'
  | 'AS_NEEDED'
  | 'EVERY_X_HOURS'
  | 'EVERY_X_DAYS'
  | 'CUSTOM';

export type ScheduleStatus = 'PENDING' | 'TAKEN' | 'SKIPPED' | 'MISSED';

export interface User {
  id: string;
  fullName: string;
  dateOfBirth?: string | null;
  gender: Gender;
  allergies?: string | null;
  chronicConditions?: string | null;
  bloodType?: string | null;
  emergencyContactName?: string | null;
  emergencyContactPhone?: string | null;
  theme: Theme;
  timeFormat: TimeFormat;
  language: Language;
  notificationsEnabled: boolean;
  alarmSoundUri?: string | null;
  alarmVibrationEnabled: boolean;
  alarmDurationSeconds: number;
  autoSnoozeEnabled: boolean;
  snoozeDurationMinutes: number;
  maxSnoozeCount: number;
  criticalAlertsEnabled: boolean;
  allowDoseSkipping: boolean;
  allowCustomDoseTime: boolean;
  hideAddMedicineNavButton: boolean;
  allowDoseUndo: boolean;
  allowDoseNotes: boolean;
  promptDoseNotes: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Medicine {
  id: string;
  userId: string;
  name: string;
  dosage: number;
  unit: DosageUnit;
  frequency: FrequencyPattern;
  instructions?: string | null;
  color: string;
  iconName: string;
  notes?: string | null;
  stockCount?: number | null;
  refillReminderThreshold?: number | null;
  scheduledTimes: string[];
  customDosages?: Record<string, number> | null;
  startDate: string;
  endDate?: string | null;
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
  status: ScheduleStatus;
  doseNotes?: string | null;
  takenTime?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PairingRequest {
  pairingCode?: string;
  role?: 'PATIENT' | 'CAREGIVER';
  patientId?: string;
}

export interface PairingResponse {
  pairingCode: string;
  expiresAt: string;
  status: 'PENDING' | 'CONFIRMED' | 'EXPIRED';
  patientId?: string;
  token?: string;
}

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

export interface SyncPushRequest {
  syncTimestamp: string;
  users?: User[];
  medicines?: Medicine[];
  schedules?: ScheduleEntry[];
}

export interface SyncPullResponse {
  syncTimestamp: string;
  users: User[];
  medicines: Medicine[];
  schedules: ScheduleEntry[];
}
