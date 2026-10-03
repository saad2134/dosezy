import Database, { type Database as DatabaseType } from 'better-sqlite3';
import { drizzle } from 'drizzle-orm/better-sqlite3';
import * as schema from './schema.js';
import { config } from '../config/index.js';
import path from 'path';
import fs from 'fs';

let sqliteDbPath = config.databaseUrl.replace('file:', '');
if (sqliteDbPath.startsWith('./')) {
  sqliteDbPath = path.resolve(process.cwd(), sqliteDbPath);
}

const dir = path.dirname(sqliteDbPath);
if (!fs.existsSync(dir)) {
  fs.mkdirSync(dir, { recursive: true });
}

export const sqlite: DatabaseType = new Database(sqliteDbPath);
sqlite.pragma('journal_mode = WAL');
sqlite.pragma('foreign_keys = ON');

// Automatically bootstrap tables if they do not exist
sqlite.exec(`
  CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    full_name TEXT NOT NULL,
    date_of_birth TEXT,
    gender TEXT NOT NULL DEFAULT 'DO_NOT_SPECIFY',
    allergies TEXT,
    chronic_conditions TEXT,
    blood_type TEXT,
    emergency_contact_name TEXT,
    emergency_contact_phone TEXT,
    theme TEXT NOT NULL DEFAULT 'SYSTEM',
    time_format TEXT NOT NULL DEFAULT 'HOUR_12',
    language TEXT NOT NULL DEFAULT 'SYSTEM',
    notifications_enabled INTEGER NOT NULL DEFAULT 1,
    alarm_sound_uri TEXT,
    alarm_vibration_enabled INTEGER NOT NULL DEFAULT 1,
    alarm_duration_seconds INTEGER NOT NULL DEFAULT 60,
    auto_snooze_enabled INTEGER NOT NULL DEFAULT 1,
    snooze_duration_minutes INTEGER NOT NULL DEFAULT 5,
    max_snooze_count INTEGER NOT NULL DEFAULT 3,
    critical_alerts_enabled INTEGER NOT NULL DEFAULT 0,
    allow_dose_skipping INTEGER NOT NULL DEFAULT 1,
    allow_custom_dose_time INTEGER NOT NULL DEFAULT 1,
    hide_add_medicine_nav_button INTEGER NOT NULL DEFAULT 0,
    allow_dose_undo INTEGER NOT NULL DEFAULT 1,
    allow_dose_notes INTEGER NOT NULL DEFAULT 1,
    prompt_dose_notes INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
  );

  CREATE TABLE IF NOT EXISTS medicines (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    name TEXT NOT NULL,
    dosage REAL NOT NULL,
    unit TEXT NOT NULL DEFAULT 'MG',
    frequency TEXT NOT NULL DEFAULT 'DAILY',
    instructions TEXT,
    color TEXT NOT NULL DEFAULT '#0284c7',
    icon_name TEXT NOT NULL DEFAULT 'PILL',
    notes TEXT,
    stock_count INTEGER,
    refill_reminder_threshold INTEGER,
    scheduled_times TEXT NOT NULL DEFAULT '[]',
    custom_dosages TEXT,
    start_date TEXT NOT NULL,
    end_date TEXT,
    active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
  );

  CREATE TABLE IF NOT EXISTS schedules (
    id TEXT PRIMARY KEY,
    medicine_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    scheduled_time TEXT NOT NULL,
    dosage REAL NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    dose_notes TEXT,
    taken_time TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
  );

  CREATE TABLE IF NOT EXISTS caregivers (
    id TEXT PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    password_hash TEXT,
    role TEXT NOT NULL DEFAULT 'family',
    specialty TEXT,
    clinic_name TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
  );

  CREATE TABLE IF NOT EXISTS pairings (
    id TEXT PRIMARY KEY,
    pairing_code TEXT NOT NULL UNIQUE,
    patient_id TEXT,
    caregiver_id TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING',
    sharing_scope TEXT NOT NULL DEFAULT 'FULL',
    expires_at TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (caregiver_id) REFERENCES caregivers(id) ON DELETE CASCADE
  );
`);


try {
  sqlite.exec(`ALTER TABLE caregivers ADD COLUMN role TEXT NOT NULL DEFAULT 'family'`);
} catch {}
try {
  sqlite.exec(`ALTER TABLE caregivers ADD COLUMN specialty TEXT`);
} catch {}
try {
  sqlite.exec(`ALTER TABLE caregivers ADD COLUMN clinic_name TEXT`);
} catch {}

export const db = drizzle(sqlite, { schema });
