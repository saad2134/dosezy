import dotenv from 'dotenv';
import path from 'path';

dotenv.config();

export const config = {
  port: parseInt(process.env.PORT || '8080', 10),
  host: process.env.HOST || '0.0.0.0',
  nodeEnv: process.env.NODE_ENV || 'development',
  databaseUrl: process.env.DATABASE_URL || 'file:./dosezy.db',
  databaseType: (process.env.DATABASE_URL?.startsWith('postgres') ? 'postgres' : 'sqlite') as 'postgres' | 'sqlite',
  e2eeSecret: process.env.E2EE_ENCRYPTION_SECRET || 'dosezy_default_secure_32_bytes_dev_secret!!',
  jwtSecret: process.env.JWT_SECRET || 'dosezy_super_secret_jwt_key_2026',
  corsOrigins: process.env.CORS_ORIGINS ? process.env.CORS_ORIGINS.split(',') : true,
};
