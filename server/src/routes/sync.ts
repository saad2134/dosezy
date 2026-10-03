import { FastifyPluginAsync } from 'fastify';
import { syncService } from '../services/syncService.js';
import { SyncPushRequest } from '../types/api.js';

export const syncRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.post<{ Body: SyncPushRequest; Querystring: { lastSyncedAt?: string } }>(
    '/v1/sync',
    async (req, reply) => {
      const lastSyncedAt = req.query.lastSyncedAt;
      const pushData = req.body || { syncTimestamp: new Date().toISOString() };

      const result = await syncService.processSync(pushData, lastSyncedAt);
      return reply.status(200).send(result);
    }
  );
};
