import { sqliteTable, text, integer, real } from 'drizzle-orm/sqlite-core';

export const users = sqliteTable('users', {
  id: text('id').primaryKey(),
  fullName: text('full_name').notNull(),
  dateOfBirth: text('date_of_birth'),
  gender: text('gender').notNull().default('DO_NOT_SPECIFY'),
  allergies: text('allergies'),
  chronicConditions: text('chronic_conditions'),
  bloodType: text('blood_type'),
  emergencyContactName: text('emergency_contact_name'),
  emergencyContactPhone: text('emergency_contact_phone'),
  theme: text('theme').notNull().default('SYSTEM'),
  timeFormat: text('time_format').notNull().default('HOUR_12'),
  language: text('language').notNull().default('SYSTEM'),
  notificationsEnabled: integer('notifications_enabled', { mode: 'boolean' }).notNull().default(true),
  alarmSoundUri: text('alarm_sound_uri'),
  alarmVibrationEnabled: integer('alarm_vibration_enabled', { mode: 'boolean' }).notNull().default(true),
  alarmDurationSeconds: integer('alarm_duration_seconds').notNull().default(60),
  autoSnoozeEnabled: integer('auto_snooze_enabled', { mode: 'boolean' }).notNull().default(true),
  snoozeDurationMinutes: integer('snooze_duration_minutes').notNull().default(5),
  maxSnoozeCount: integer('max_snooze_count').notNull().default(3),
  criticalAlertsEnabled: integer('critical_alerts_enabled', { mode: 'boolean' }).notNull().default(false),
  allowDoseSkipping: integer('allow_dose_skipping', { mode: 'boolean' }).notNull().default(true),
  allowCustomDoseTime: integer('allow_custom_dose_time', { mode: 'boolean' }).notNull().default(true),
  hideAddMedicineNavButton: integer('hide_add_medicine_nav_button', { mode: 'boolean' }).notNull().default(false),
  allowDoseUndo: integer('allow_dose_undo', { mode: 'boolean' }).notNull().default(true),
  allowDoseNotes: integer('allow_dose_notes', { mode: 'boolean' }).notNull().default(true),
  promptDoseNotes: integer('prompt_dose_notes', { mode: 'boolean' }).notNull().default(false),
  createdAt: text('created_at').notNull(),
  updatedAt: text('updated_at').notNull(),
});

export const medicines = sqliteTable('medicines', {
  id: text('id').primaryKey(),
  userId: text('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  name: text('name').notNull(),
  dosage: real('dosage').notNull(),
  unit: text('unit').notNull().default('MG'),
  frequency: text('frequency').notNull().default('DAILY'),
  instructions: text('instructions'),
  color: text('color').notNull().default('#0284c7'),
  iconName: text('icon_name').notNull().default('PILL'),
  notes: text('notes'),
  stockCount: integer('stock_count'),
  refillReminderThreshold: integer('refill_reminder_threshold'),
  scheduledTimes: text('scheduled_times').notNull().default('[]'), // JSON array
  customDosages: text('custom_dosages'), // JSON object
  startDate: text('start_date').notNull(),
  endDate: text('end_date'),
  active: integer('active', { mode: 'boolean' }).notNull().default(true),
  createdAt: text('created_at').notNull(),
  updatedAt: text('updated_at').notNull(),
});

export const schedules = sqliteTable('schedules', {
  id: text('id').primaryKey(),
  medicineId: text('medicine_id').notNull().references(() => medicines.id, { onDelete: 'cascade' }),
  userId: text('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  scheduledTime: text('scheduled_time').notNull(),
  dosage: real('dosage').notNull(),
  status: text('status').notNull().default('PENDING'),
  doseNotes: text('dose_notes'),
  takenTime: text('taken_time'),
  createdAt: text('created_at').notNull(),
  updatedAt: text('updated_at').notNull(),
});

export const caregivers = sqliteTable('caregivers', {
  id: text('id').primaryKey(),
  email: text('email').notNull().unique(),
  name: text('name').notNull(),
  passwordHash: text('password_hash'),
  role: text('role').notNull().default('family'), // 'family' | 'doctor'
  specialty: text('specialty'),
  clinicName: text('clinic_name'),
  createdAt: text('created_at').notNull(),
  updatedAt: text('updated_at').notNull(),
});

export const pairings = sqliteTable('pairings', {
  id: text('id').primaryKey(),
  pairingCode: text('pairing_code').notNull().unique(),
  patientId: text('patient_id').references(() => users.id, { onDelete: 'cascade' }),
  caregiverId: text('caregiver_id').references(() => caregivers.id, { onDelete: 'cascade' }),
  status: text('status').notNull().default('PENDING'),
  sharingScope: text('sharing_scope').notNull().default('FULL'),
  expiresAt: text('expires_at').notNull(),
  createdAt: text('created_at').notNull(),
});
