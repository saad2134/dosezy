import { FastifyReply } from 'fastify';

interface SSEClient {
  id: string;
  reply: FastifyReply;
  caregiverId?: string;
}

class RealtimeService {
  private clients: Map<string, SSEClient> = new Map();

  addClient(id: string, reply: FastifyReply, caregiverId?: string) {
    this.clients.set(id, { id, reply, caregiverId });

    // Send initial ping to keep connection open
    this.sendToClient(id, 'connected', { status: 'ok', clientId: id, timestamp: new Date().toISOString() });

    reply.raw.on('close', () => {
      this.removeClient(id);
    });
  }

  removeClient(id: string) {
    this.clients.delete(id);
  }

  sendToClient(clientId: string, event: string, data: unknown) {
    const client = this.clients.get(clientId);
    if (client && !client.reply.raw.destroyed) {
      try {
        client.reply.raw.write(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`);
      } catch (err) {
        this.removeClient(clientId);
      }
    }
  }

  broadcast(event: string, data: unknown) {
    for (const [id, client] of this.clients.entries()) {
      if (!client.reply.raw.destroyed) {
        try {
          client.reply.raw.write(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`);
        } catch (err) {
          this.removeClient(id);
        }
      } else {
        this.removeClient(id);
      }
    }
  }

  getClientCount(): number {
    return this.clients.size;
  }
}

export const realtimeService = new RealtimeService();
