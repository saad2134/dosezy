import { FastifyPluginAsync } from 'fastify';
import { db } from '../db/index.js';
import { caregivers } from '../db/schema.js';
import { eq } from 'drizzle-orm';
import crypto from 'crypto';

function hashPassword(password: string): string {
  return crypto.createHash('sha256').update(password).digest('hex');
}

export const authRoutes: FastifyPluginAsync = async (fastify) => {
  // Register Caregiver / Healthcare Provider
  fastify.post<{
    Body: {
      email: string;
      name: string;
      password?: string;
      role?: 'family' | 'doctor';
      specialty?: string;
      clinicName?: string;
    };
  }>('/v1/auth/register', async (req, reply) => {
    const { email, name, password, role = 'family', specialty, clinicName } = req.body || {};

    if (!email || !name) {
      return reply.status(400).send({
        error: 'VALIDATION_ERROR',
        message: 'Name and email are required.',
      });
    }

    const cleanEmail = email.trim().toLowerCase();
    const existing = db.select().from(caregivers).where(eq(caregivers.email, cleanEmail)).get();

    if (existing) {
      return reply.status(409).send({
        error: 'USER_EXISTS',
        message: 'An account with this email already exists.',
      });
    }

    const id = crypto.randomUUID();
    const now = new Date().toISOString();
    const passwordHash = password ? hashPassword(password) : null;

    db.insert(caregivers).values({
      id,
      email: cleanEmail,
      name: name.trim(),
      passwordHash,
      role: role === 'doctor' ? 'doctor' : 'family',
      specialty: specialty?.trim() || null,
      clinicName: clinicName?.trim() || null,
      createdAt: now,
      updatedAt: now,
    }).run();

    const created = db.select().from(caregivers).where(eq(caregivers.id, id)).get();

    return reply.status(201).send({
      id: created?.id,
      email: created?.email,
      name: created?.name,
      role: created?.role || 'family',
      specialty: created?.specialty || null,
      clinicName: created?.clinicName || null,
      token: `cg_token_${id.replace(/-/g, '')}`,
    });
  });

  // Login Caregiver / Healthcare Provider
  fastify.post<{ Body: { email: string; password?: string } }>('/v1/auth/login', async (req, reply) => {
    const { email, password } = req.body || {};

    if (!email) {
      return reply.status(400).send({
        error: 'VALIDATION_ERROR',
        message: 'Email is required.',
      });
    }

    const cleanEmail = email.trim().toLowerCase();
    const user = db.select().from(caregivers).where(eq(caregivers.email, cleanEmail)).get();

    if (!user) {
      return reply.status(404).send({
        error: 'USER_NOT_FOUND',
        message: 'No account found with this email. Please register first.',
      });
    }

    if (user.passwordHash && password) {
      const incomingHash = hashPassword(password);
      if (incomingHash !== user.passwordHash) {
        return reply.status(401).send({
          error: 'INVALID_CREDENTIALS',
          message: 'Incorrect password entered.',
        });
      }
    }

    return reply.status(200).send({
      id: user.id,
      email: user.email,
      name: user.name,
      role: user.role || 'family',
      specialty: user.specialty || null,
      clinicName: user.clinicName || null,
      token: `cg_token_${user.id.replace(/-/g, '')}`,
    });
  });

  // Current Caregiver Session
  fastify.get('/v1/auth/me', async (req, reply) => {
    const caregiverId = req.headers['x-caregiver-id'] as string;
    if (!caregiverId) {
      return reply.status(401).send({ error: 'UNAUTHORIZED', message: 'Not authenticated' });
    }

    const user = db.select().from(caregivers).where(eq(caregivers.id, caregiverId)).get();
    if (!user) {
      return reply.status(404).send({ error: 'USER_NOT_FOUND', message: 'User not found' });
    }

    return reply.status(200).send({
      id: user.id,
      email: user.email,
      name: user.name,
      role: user.role || 'family',
      specialty: user.specialty || null,
      clinicName: user.clinicName || null,
    });
  });
};
