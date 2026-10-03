import { FastifyPluginAsync } from 'fastify';
import { db } from '../db/index.js';
import { users } from '../db/schema.js';
import { eq } from 'drizzle-orm';
import { User } from '../types/api.js';

export const userRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.get('/v1/users', async (_req, reply) => {
    const allUsers = db.select().from(users).all();
    return reply.status(200).send(allUsers);
  });

  fastify.get<{ Params: { id: string } }>('/v1/users/:id', async (req, reply) => {
    const user = db.select().from(users).where(eq(users.id, req.params.id)).get();
    if (!user) {
      return reply.status(404).send({ error: 'NOT_FOUND', message: 'User not found' });
    }
    return reply.status(200).send(user);
  });

  fastify.post<{ Body: Partial<User> }>('/v1/users', async (req, reply) => {
    const now = new Date().toISOString();
    const id = req.body.id || crypto.randomUUID();
    const payload = {
      id,
      fullName: req.body.fullName || 'Anonymous Patient',
      dateOfBirth: req.body.dateOfBirth,
      gender: req.body.gender || 'DO_NOT_SPECIFY',
      allergies: req.body.allergies,
      chronicConditions: req.body.chronicConditions,
      bloodType: req.body.bloodType,
      emergencyContactName: req.body.emergencyContactName,
      emergencyContactPhone: req.body.emergencyContactPhone,
      theme: req.body.theme || 'SYSTEM',
      timeFormat: req.body.timeFormat || 'HOUR_12',
      language: req.body.language || 'SYSTEM',
      notificationsEnabled: req.body.notificationsEnabled ?? true,
      alarmSoundUri: req.body.alarmSoundUri,
      alarmVibrationEnabled: req.body.alarmVibrationEnabled ?? true,
      alarmDurationSeconds: req.body.alarmDurationSeconds ?? 60,
      autoSnoozeEnabled: req.body.autoSnoozeEnabled ?? true,
      snoozeDurationMinutes: req.body.snoozeDurationMinutes ?? 5,
      maxSnoozeCount: req.body.maxSnoozeCount ?? 3,
      criticalAlertsEnabled: req.body.criticalAlertsEnabled ?? false,
      allowDoseSkipping: req.body.allowDoseSkipping ?? true,
      allowCustomDoseTime: req.body.allowCustomDoseTime ?? true,
      hideAddMedicineNavButton: req.body.hideAddMedicineNavButton ?? false,
      allowDoseUndo: req.body.allowDoseUndo ?? true,
      allowDoseNotes: req.body.allowDoseNotes ?? true,
      promptDoseNotes: req.body.promptDoseNotes ?? false,
      createdAt: req.body.createdAt || now,
      updatedAt: req.body.updatedAt || now,
    };

    db.insert(users).values(payload).run();
    const created = db.select().from(users).where(eq(users.id, id)).get();
    return reply.status(201).send(created);
  });

  fastify.put<{ Params: { id: string }; Body: Partial<User> }>('/v1/users/:id', async (req, reply) => {
    const existing = db.select().from(users).where(eq(users.id, req.params.id)).get();
    if (!existing) {
      return reply.status(404).send({ error: 'NOT_FOUND', message: 'User not found' });
    }

    const now = new Date().toISOString();
    db.update(users).set({
      ...req.body,
      updatedAt: now,
    }).where(eq(users.id, req.params.id)).run();

    const updated = db.select().from(users).where(eq(users.id, req.params.id)).get();
    return reply.status(200).send(updated);
  });

  fastify.delete<{ Params: { id: string } }>('/v1/users/:id', async (req, reply) => {
    db.delete(users).where(eq(users.id, req.params.id)).run();
    return reply.status(204).send();
  });
};
