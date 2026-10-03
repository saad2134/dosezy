import { FastifyPluginAsync } from 'fastify';
import { db } from '../db/index.js';
import { schedules, medicines } from '../db/schema.js';
import { eq, and } from 'drizzle-orm';
import { realtimeService } from '../services/realtimeService.js';

export const scheduleRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.get('/v1/schedules', async (_req, reply) => {
    const list = db.select().from(schedules).all();
    return reply.status(200).send(list);
  });

  fastify.get<{ Params: { userId: string } }>('/v1/users/:userId/schedules', async (req, reply) => {
    const list = db.select().from(schedules).where(eq(schedules.userId, req.params.userId)).all();
    return reply.status(200).send(list);
  });

  fastify.post<{ Params: { id: string }; Body: { takenTime?: string; doseNotes?: string } }>(
    '/v1/schedules/:id/take',
    async (req, reply) => {
      const existing = db.select().from(schedules).where(eq(schedules.id, req.params.id)).get();
      if (!existing) {
        return reply.status(404).send({ error: 'NOT_FOUND', message: 'Schedule entry not found' });
      }

      const now = new Date().toISOString();
      db.update(schedules).set({
        status: 'TAKEN',
        takenTime: req.body?.takenTime || now,
        doseNotes: req.body?.doseNotes || existing.doseNotes,
        updatedAt: now,
      }).where(eq(schedules.id, req.params.id)).run();

      const updated = db.select().from(schedules).where(eq(schedules.id, req.params.id)).get();

      realtimeService.broadcast('dose_taken', {
        scheduleId: req.params.id,
        userId: existing.userId,
        medicineId: existing.medicineId,
        status: 'TAKEN',
        takenTime: updated?.takenTime,
        timestamp: now,
      });

      return reply.status(200).send(updated);
    }
  );

  fastify.post<{ Params: { id: string } }>('/v1/schedules/:id/undo', async (req, reply) => {
    const existing = db.select().from(schedules).where(eq(schedules.id, req.params.id)).get();
    if (!existing) {
      return reply.status(404).send({ error: 'NOT_FOUND', message: 'Schedule entry not found' });
    }

    const now = new Date().toISOString();
    db.update(schedules).set({
      status: 'PENDING',
      takenTime: null,
      updatedAt: now,
    }).where(eq(schedules.id, req.params.id)).run();

    const updated = db.select().from(schedules).where(eq(schedules.id, req.params.id)).get();

    realtimeService.broadcast('dose_undo', {
      scheduleId: req.params.id,
      userId: existing.userId,
      medicineId: existing.medicineId,
      status: 'PENDING',
      timestamp: now,
    });

    return reply.status(200).send(updated);
  });
};
