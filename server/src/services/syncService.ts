import { db } from '../db/index.js';
import { users, medicines, schedules } from '../db/schema.js';
import { eq, gte, sql } from 'drizzle-orm';
import { SyncPushRequest, SyncPullResponse, User, Medicine, ScheduleEntry } from '../types/api.js';
import { realtimeService } from './realtimeService.js';

export class SyncService {
  async processSync(pushData: SyncPushRequest, lastSyncedAt?: string): Promise<SyncPullResponse> {
    const now = new Date().toISOString();

    // 1. Process Users Upsert
    if (pushData.users && pushData.users.length > 0) {
      for (const u of pushData.users) {
        const existing = db.select().from(users).where(eq(users.id, u.id)).get();
        if (!existing) {
          db.insert(users).values({
            id: u.id,
            fullName: u.fullName,
            dateOfBirth: u.dateOfBirth,
            gender: u.gender,
            allergies: u.allergies,
            chronicConditions: u.chronicConditions,
            bloodType: u.bloodType,
            emergencyContactName: u.emergencyContactName,
            emergencyContactPhone: u.emergencyContactPhone,
            theme: u.theme,
            timeFormat: u.timeFormat,
            language: u.language,
            notificationsEnabled: u.notificationsEnabled,
            alarmSoundUri: u.alarmSoundUri,
            alarmVibrationEnabled: u.alarmVibrationEnabled,
            alarmDurationSeconds: u.alarmDurationSeconds,
            autoSnoozeEnabled: u.autoSnoozeEnabled,
            snoozeDurationMinutes: u.snoozeDurationMinutes,
            maxSnoozeCount: u.maxSnoozeCount,
            criticalAlertsEnabled: u.criticalAlertsEnabled,
            allowDoseSkipping: u.allowDoseSkipping,
            allowCustomDoseTime: u.allowCustomDoseTime,
            hideAddMedicineNavButton: u.hideAddMedicineNavButton,
            allowDoseUndo: u.allowDoseUndo,
            allowDoseNotes: u.allowDoseNotes,
            promptDoseNotes: u.promptDoseNotes,
            createdAt: u.createdAt || now,
            updatedAt: u.updatedAt || now,
          }).run();
        } else if (new Date(u.updatedAt).getTime() >= new Date(existing.updatedAt).getTime()) {
          db.update(users).set({
            fullName: u.fullName,
            dateOfBirth: u.dateOfBirth,
            gender: u.gender,
            allergies: u.allergies,
            chronicConditions: u.chronicConditions,
            bloodType: u.bloodType,
            emergencyContactName: u.emergencyContactName,
            emergencyContactPhone: u.emergencyContactPhone,
            theme: u.theme,
            timeFormat: u.timeFormat,
            language: u.language,
            notificationsEnabled: u.notificationsEnabled,
            alarmSoundUri: u.alarmSoundUri,
            alarmVibrationEnabled: u.alarmVibrationEnabled,
            alarmDurationSeconds: u.alarmDurationSeconds,
            autoSnoozeEnabled: u.autoSnoozeEnabled,
            snoozeDurationMinutes: u.snoozeDurationMinutes,
            maxSnoozeCount: u.maxSnoozeCount,
            criticalAlertsEnabled: u.criticalAlertsEnabled,
            allowDoseSkipping: u.allowDoseSkipping,
            allowCustomDoseTime: u.allowCustomDoseTime,
            hideAddMedicineNavButton: u.hideAddMedicineNavButton,
            allowDoseUndo: u.allowDoseUndo,
            allowDoseNotes: u.allowDoseNotes,
            promptDoseNotes: u.promptDoseNotes,
            updatedAt: u.updatedAt || now,
          }).where(eq(users.id, u.id)).run();
        }
      }
    }

    // 2. Process Medicines Upsert
    if (pushData.medicines && pushData.medicines.length > 0) {
      for (const m of pushData.medicines) {
        const existing = db.select().from(medicines).where(eq(medicines.id, m.id)).get();
        const scheduledTimesStr = JSON.stringify(m.scheduledTimes || []);
        const customDosagesStr = m.customDosages ? JSON.stringify(m.customDosages) : null;

        if (!existing) {
          db.insert(medicines).values({
            id: m.id,
            userId: m.userId,
            name: m.name,
            dosage: m.dosage,
            unit: m.unit,
            frequency: m.frequency,
            instructions: m.instructions,
            color: m.color,
            iconName: m.iconName,
            notes: m.notes,
            stockCount: m.stockCount,
            refillReminderThreshold: m.refillReminderThreshold,
            scheduledTimes: scheduledTimesStr,
            customDosages: customDosagesStr,
            startDate: m.startDate,
            endDate: m.endDate,
            active: m.active,
            createdAt: m.createdAt || now,
            updatedAt: m.updatedAt || now,
          }).run();
        } else if (new Date(m.updatedAt).getTime() >= new Date(existing.updatedAt).getTime()) {
          db.update(medicines).set({
            name: m.name,
            dosage: m.dosage,
            unit: m.unit,
            frequency: m.frequency,
            instructions: m.instructions,
            color: m.color,
            iconName: m.iconName,
            notes: m.notes,
            stockCount: m.stockCount,
            refillReminderThreshold: m.refillReminderThreshold,
            scheduledTimes: scheduledTimesStr,
            customDosages: customDosagesStr,
            startDate: m.startDate,
            endDate: m.endDate,
            active: m.active,
            updatedAt: m.updatedAt || now,
          }).where(eq(medicines.id, m.id)).run();
        }
      }
    }

    // 3. Process Schedules Upsert
    let scheduleUpdated = false;
    if (pushData.schedules && pushData.schedules.length > 0) {
      for (const s of pushData.schedules) {
        const existing = db.select().from(schedules).where(eq(schedules.id, s.id)).get();
        if (!existing) {
          db.insert(schedules).values({
            id: s.id,
            medicineId: s.medicineId,
            userId: s.userId,
            scheduledTime: s.scheduledTime,
            dosage: s.dosage,
            status: s.status,
            doseNotes: s.doseNotes,
            takenTime: s.takenTime,
            createdAt: s.createdAt || now,
            updatedAt: s.updatedAt || now,
          }).run();
          scheduleUpdated = true;
        } else if (new Date(s.updatedAt).getTime() >= new Date(existing.updatedAt).getTime()) {
          db.update(schedules).set({
            scheduledTime: s.scheduledTime,
            dosage: s.dosage,
            status: s.status,
            doseNotes: s.doseNotes,
            takenTime: s.takenTime,
            updatedAt: s.updatedAt || now,
          }).where(eq(schedules.id, s.id)).run();
          scheduleUpdated = true;
        }
      }
    }

    if (scheduleUpdated) {
      realtimeService.broadcast('schedules_updated', {
        timestamp: now,
        count: pushData.schedules?.length || 0,
      });
    }

    // 4. Build Pull Response
    const filterTime = lastSyncedAt ? new Date(lastSyncedAt).toISOString() : null;

    const allUsers = filterTime
      ? db.select().from(users).where(gte(users.updatedAt, filterTime)).all()
      : db.select().from(users).all();

    const allMeds = filterTime
      ? db.select().from(medicines).where(gte(medicines.updatedAt, filterTime)).all()
      : db.select().from(medicines).all();

    const allScheds = filterTime
      ? db.select().from(schedules).where(gte(schedules.updatedAt, filterTime)).all()
      : db.select().from(schedules).all();

    return {
      syncTimestamp: now,
      users: allUsers as unknown as User[],
      medicines: allMeds.map(m => ({
        ...m,
        scheduledTimes: JSON.parse(m.scheduledTimes || '[]'),
        customDosages: m.customDosages ? JSON.parse(m.customDosages) : null,
      })) as unknown as Medicine[],
      schedules: allScheds as unknown as ScheduleEntry[],
    };
  }
}

export const syncService = new SyncService();
