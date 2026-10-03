import { FastifyPluginAsync } from 'fastify';

export const healthRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.get('/health', async (_req, reply) => {
    return reply.status(200).send({
      status: 'ok',
      version: '1.0.0',
      service: 'dosezy-server',
      uptime: Math.floor(process.uptime()),
      timestamp: new Date().toISOString(),
    });
  });
};
