import { FastifyPluginAsync } from 'fastify';
import { db } from '../db/index.js';
import { medicines } from '../db/schema.js';
import { eq } from 'drizzle-orm';
import { Medicine } from '../types/api.js';

export const medicineRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.get('/v1/medicines', async (_req, reply) => {
    const list = db.select().from(medicines).all();
    const formatted = list.map(m => ({
      ...m,
      scheduledTimes: JSON.parse(m.scheduledTimes || '[]'),
      customDosages: m.customDosages ? JSON.parse(m.customDosages) : null,
    }));
    return reply.status(200).send(formatted);
  });

  fastify.get<{ Params: { id: string } }>('/v1/medicines/:id', async (req, reply) => {
    const med = db.select().from(medicines).where(eq(medicines.id, req.params.id)).get();
    if (!med) {
      return reply.status(404).send({ error: 'NOT_FOUND', message: 'Medicine not found' });
    }
    return reply.status(200).send({
      ...med,
      scheduledTimes: JSON.parse(med.scheduledTimes || '[]'),
      customDosages: med.customDosages ? JSON.parse(med.customDosages) : null,
    });
  });

  fastify.post<{ Body: Partial<Medicine> }>('/v1/medicines', async (req, reply) => {
    const now = new Date().toISOString();
    const id = req.body.id || crypto.randomUUID();
    const scheduledTimesStr = JSON.stringify(req.body.scheduledTimes || ['08:00']);
    const customDosagesStr = req.body.customDosages ? JSON.stringify(req.body.customDosages) : null;

    db.insert(medicines).values({
      id,
      userId: req.body.userId || 'default-user',
      name: req.body.name || 'Unnamed Medication',
      dosage: req.body.dosage || 1,
      unit: req.body.unit || 'MG',
      frequency: req.body.frequency || 'DAILY',
      instructions: req.body.instructions,
      color: req.body.color || '#0284c7',
      iconName: req.body.iconName || 'PILL',
      notes: req.body.notes,
      stockCount: req.body.stockCount,
      refillReminderThreshold: req.body.refillReminderThreshold,
      scheduledTimes: scheduledTimesStr,
      customDosages: customDosagesStr,
      startDate: req.body.startDate || now.slice(0, 10),
      endDate: req.body.endDate,
      active: req.body.active ?? true,
      createdAt: req.body.createdAt || now,
      updatedAt: req.body.updatedAt || now,
    }).run();

    const created = db.select().from(medicines).where(eq(medicines.id, id)).get();
    return reply.status(201).send({
      ...created,
      scheduledTimes: JSON.parse(created?.scheduledTimes || '[]'),
      customDosages: created?.customDosages ? JSON.parse(created.customDosages) : null,
    });
  });

  fastify.delete<{ Params: { id: string } }>('/v1/medicines/:id', async (req, reply) => {
    db.delete(medicines).where(eq(medicines.id, req.params.id)).run();
    return reply.status(204).send();
  });
};
