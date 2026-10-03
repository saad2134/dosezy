import { FastifyPluginAsync } from 'fastify';
import { pairingService } from '../services/pairingService.js';
import { realtimeService } from '../services/realtimeService.js';
import { PairingRequest } from '../types/api.js';
import crypto from 'crypto';

export const caregiverRoutes: FastifyPluginAsync = async (fastify) => {
  // Generate or redeem pairing code
  fastify.post<{ Body: PairingRequest }>('/v1/caregiver/pair', async (req, reply) => {
    const { pairingCode, role, patientId } = req.body || {};

    if (role === 'PATIENT' || (!pairingCode && patientId)) {
      // Patient initiates pairing: generate new code
      const response = await pairingService.createPairingCode(patientId);
      return reply.status(200).send(response);
    } else if (pairingCode) {
      // Caregiver redeems code
      const caregiverId = (req.headers['x-caregiver-id'] as string) || 'default-caregiver';
      const result = await pairingService.redeemPairingCode(pairingCode, caregiverId);

      if (!result.success) {
        return reply.status(400).send({
          error: 'PAIRING_FAILED',
          message: result.message,
        });
      }

      return reply.status(200).send({
        pairingCode,
        status: 'CONFIRMED',
        patientId: result.pairing?.patientId,
      });
    }

    return reply.status(400).send({
      error: 'INVALID_REQUEST',
      message: 'Provide patientId to generate code, or pairingCode to redeem.',
    });
  });

  // List monitored patients & adherence metrics
  fastify.get('/v1/caregiver/patients', async (req, reply) => {
    const caregiverId = req.headers['x-caregiver-id'] as string | undefined;
    const patients = await pairingService.getMonitoredPatients(caregiverId);
    return reply.status(200).send(patients);
  });

  // Server-Sent Events (SSE) stream for live updates
  fastify.get('/v1/caregiver/events', async (req, reply) => {
    const clientId = crypto.randomUUID();
    const caregiverId = req.headers['x-caregiver-id'] as string | undefined;

    reply.raw.setHeader('Content-Type', 'text/event-stream');
    reply.raw.setHeader('Cache-Control', 'no-cache');
    reply.raw.setHeader('Connection', 'keep-alive');
    reply.raw.flushHeaders();

    realtimeService.addClient(clientId, reply, caregiverId);
  });
};
