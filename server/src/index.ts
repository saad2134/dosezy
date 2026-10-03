import Fastify from 'fastify';
import cors from '@fastify/cors';
import swagger from '@fastify/swagger';
import swaggerUi from '@fastify/swagger-ui';
import { config } from './config/index.js';
import { healthRoutes } from './routes/health.js';
import { authRoutes } from './routes/auth.js';
import { caregiverRoutes } from './routes/caregiver.js';
import { syncRoutes } from './routes/sync.js';
import { userRoutes } from './routes/users.js';
import { medicineRoutes } from './routes/medicines.js';
import { scheduleRoutes } from './routes/schedules.js';
import { db } from './db/index.js';
import { users, medicines, schedules } from './db/schema.js';

export async function buildApp() {
  const app = Fastify({
    logger: {
      level: config.nodeEnv === 'development' ? 'info' : 'warn',
    },
  });

  // Enable CORS
  await app.register(cors, {
    origin: config.corsOrigins,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    credentials: true,
  });

  // OpenAPI Swagger Documentation
  await app.register(swagger, {
    openapi: {
      info: {
        title: 'Dosezy Server API',
        description: 'Self-Hostable Delta-Sync and Caregiver Backend Server for Dosezy',
        version: '1.0.0',
      },
      servers: [
        { url: `http://${config.host}:${config.port}`, description: 'Local Server' },
      ],
    },
  });

  await app.register(swaggerUi, {
    routePrefix: '/docs',
    uiConfig: {
      docExpansion: 'list',
      deepLinking: false,
    },
  });

  // Register Routes
  await app.register(healthRoutes);
  await app.register(authRoutes);
  await app.register(caregiverRoutes);
  await app.register(syncRoutes);
  await app.register(userRoutes);
  await app.register(medicineRoutes);
  await app.register(scheduleRoutes);

  // Seed sample patient if DB is empty for immediate out-of-the-box demo
  seedSampleData();

  return app;
}

function seedSampleData() {
  try {
    const existingUsers = db.select().from(users).all();
    if (existingUsers.length === 0) {
      const now = new Date().toISOString();
      const patientId = 'demo-patient-001';

      db.insert(users).values({
        id: patientId,
        fullName: 'Eleanor Vance',
        dateOfBirth: '1952-04-12',
        gender: 'FEMALE',
        allergies: 'Penicillin, Sulfa drugs',
        chronicConditions: 'Hypertension, Type 2 Diabetes',
        bloodType: 'A+',
        emergencyContactName: 'Robert Vance (Son)',
        emergencyContactPhone: '+1 (555) 234-5678',
        theme: 'DARK',
        timeFormat: 'HOUR_12',
        language: 'ENGLISH',
        notificationsEnabled: true,
        alarmVibrationEnabled: true,
        alarmDurationSeconds: 15,
        autoSnoozeEnabled: true,
        snoozeDurationMinutes: 5,
        maxSnoozeCount: 3,
        criticalAlertsEnabled: true,
        allowDoseSkipping: true,
        allowCustomDoseTime: true,
        hideAddMedicineNavButton: false,
        allowDoseUndo: true,
        allowDoseNotes: true,
        promptDoseNotes: false,
        createdAt: now,
        updatedAt: now,
      }).run();

      const med1Id = 'med-lisinopril';
      db.insert(medicines).values({
        id: med1Id,
        userId: patientId,
        name: 'Lisinopril',
        dosage: 10,
        unit: 'MG',
        frequency: 'DAILY',
        instructions: 'Take 1 tablet every morning with a full glass of water',
        color: '#0284c7',
        iconName: 'PILL',
        stockCount: 28,
        refillReminderThreshold: 5,
        scheduledTimes: JSON.stringify(['08:00']),
        startDate: now.slice(0, 10),
        active: true,
        createdAt: now,
        updatedAt: now,
      }).run();

      const med2Id = 'med-metformin';
      db.insert(medicines).values({
        id: med2Id,
        userId: patientId,
        name: 'Metformin HCl',
        dosage: 500,
        unit: 'MG',
        frequency: 'DAILY',
        instructions: 'Take with evening meal',
        color: '#8b5cf6',
        iconName: 'CAPSULE',
        stockCount: 45,
        refillReminderThreshold: 10,
        scheduledTimes: JSON.stringify(['20:00']),
        startDate: now.slice(0, 10),
        active: true,
        createdAt: now,
        updatedAt: now,
      }).run();

      // Today's sample schedules
      const todayDate = now.slice(0, 10);
      db.insert(schedules).values({
        id: 'sched-1',
        medicineId: med1Id,
        userId: patientId,
        scheduledTime: `${todayDate}T08:00:00.000Z`,
        dosage: 10,
        status: 'TAKEN',
        takenTime: `${todayDate}T08:05:12.000Z`,
        doseNotes: 'Taken with breakfast',
        createdAt: now,
        updatedAt: now,
      }).run();

      db.insert(schedules).values({
        id: 'sched-2',
        medicineId: med2Id,
        userId: patientId,
        scheduledTime: `${todayDate}T20:00:00.000Z`,
        dosage: 500,
        status: 'PENDING',
        createdAt: now,
        updatedAt: now,
      }).run();
    }
  } catch (err) {
    // Ignore seed errors if already populated
  }
}

async function start() {
  try {
    const app = await buildApp();
    await app.listen({ port: config.port, host: config.host });
    console.log(`\n🚀 Dosezy Server is running at http://${config.host}:${config.port}`);
    console.log(`📖 OpenAPI Interactive Docs available at http://${config.host}:${config.port}/docs\n`);
  } catch (err) {
    console.error('Failed to start server:', err);
    process.exit(1);
  }
}

// Start if executed directly
if (process.argv[1]?.endsWith('index.ts') || process.argv[1]?.endsWith('index.js')) {
  start();
}
