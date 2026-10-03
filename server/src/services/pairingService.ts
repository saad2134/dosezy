import crypto from 'crypto';
import { db } from '../db/index.js';
import { pairings, users, medicines, schedules, caregivers } from '../db/schema.js';
import { eq, and, sql } from 'drizzle-orm';
import { CaregiverPatientSummary, PairingResponse } from '../types/api.js';
import { realtimeService } from './realtimeService.js';

export class PairingService {
  generateCode(): string {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // Avoid visually ambiguous chars like 0/O, 1/I
    let code = '';
    const bytes = crypto.randomBytes(6);
    for (let i = 0; i < 6; i++) {
      code += chars[bytes[i] % chars.length];
    }
    return code;
  }

  async createPairingCode(patientId?: string): Promise<PairingResponse> {
    const code = this.generateCode();
    const expiresAt = new Date(Date.now() + 15 * 60 * 1000).toISOString(); // 15 mins expiry
    const id = crypto.randomUUID();
    const now = new Date().toISOString();

    db.insert(pairings).values({
      id,
      pairingCode: code,
      patientId: patientId || null,
      status: 'PENDING',
      sharingScope: 'FULL',
      expiresAt,
      createdAt: now,
    }).run();

    return {
      pairingCode: code,
      expiresAt,
      status: 'PENDING',
      patientId,
    };
  }

  async redeemPairingCode(code: string, caregiverId: string): Promise<{ success: boolean; message: string; pairing?: typeof pairings.$inferSelect }> {
    const cleanCode = code.trim().toUpperCase();
    const existing = db.select().from(pairings).where(eq(pairings.pairingCode, cleanCode)).get();

    if (!existing) {
      return { success: false, message: 'Invalid pairing code.' };
    }

    if (new Date(existing.expiresAt).getTime() < Date.now()) {
      return { success: false, message: 'Pairing code has expired.' };
    }

    // Ensure caregiver exists, or create a default guest caregiver if not present
    let caregiver = db.select().from(caregivers).where(eq(caregivers.id, caregiverId)).get();
    if (!caregiver) {
      const now = new Date().toISOString();
      db.insert(caregivers).values({
        id: caregiverId,
        email: `caregiver_${caregiverId.slice(0, 8)}@dosezy.local`,
        name: 'Primary Caregiver',
        createdAt: now,
        updatedAt: now,
      }).run();
    }

    db.update(pairings)
      .set({
        caregiverId,
        status: 'CONFIRMED',
      })
      .where(eq(pairings.id, existing.id))
      .run();

    const updated = db.select().from(pairings).where(eq(pairings.id, existing.id)).get();

    realtimeService.broadcast('pairing_confirmed', {
      pairingCode: cleanCode,
      patientId: existing.patientId,
      caregiverId,
      timestamp: new Date().toISOString(),
    });

    return { success: true, message: 'Successfully paired with patient.', pairing: updated };
  }

  async getMonitoredPatients(caregiverId?: string): Promise<CaregiverPatientSummary[]> {
    // If caregiverId provided, get linked patients; otherwise return all registered patients for self-host demo
    let patientIds: string[] = [];

    if (caregiverId) {
      const links = db.select().from(pairings).where(
        and(eq(pairings.caregiverId, caregiverId), eq(pairings.status, 'CONFIRMED'))
      ).all();
      patientIds = links.map(l => l.patientId).filter((id): id is string => Boolean(id));
    }

    // If no specific links found or self-hosted default, list all users in system
    let targetUsers = patientIds.length > 0
      ? db.select().from(users).all().filter(u => patientIds.includes(u.id))
      : db.select().from(users).all();

    const summaries: CaregiverPatientSummary[] = [];

    for (const u of targetUsers) {
      const userMeds = db.select().from(medicines).where(eq(medicines.userId, u.id)).all();
      const userScheds = db.select().from(schedules).where(eq(schedules.userId, u.id)).all();

      const activeMedsCount = userMeds.filter(m => m.active).length;

      let takenCount = 0;
      let missedCount = 0;
      let pendingCount = 0;
      let lateCount = 0;

      for (const s of userScheds) {
        if (s.status === 'TAKEN') {
          takenCount++;
        } else if (s.status === 'MISSED') {
          missedCount++;
        } else if (s.status === 'PENDING') {
          pendingCount++;
        } else if (s.status === 'LATE' as any) {
          lateCount++;
        }
      }

      const totalDoses = takenCount + missedCount;
      const adherenceRate = totalDoses > 0 ? Math.round((takenCount / totalDoses) * 1000) / 10 : 100.0;

      summaries.push({
        userId: u.id,
        fullName: u.fullName,
        adherenceRate,
        activeMedicinesCount: activeMedsCount,
        missedDosesCount: missedCount,
        todayPendingCount: pendingCount,
        todayTakenCount: takenCount,
        todayLateCount: lateCount,
        allergies: u.allergies,
        chronicConditions: u.chronicConditions,
        emergencyContactName: u.emergencyContactName,
        emergencyContactPhone: u.emergencyContactPhone,
        lastSyncedAt: u.updatedAt,
      });
    }

    return summaries;
  }
}

export const pairingService = new PairingService();
