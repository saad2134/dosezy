/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dosezy.data.converters.Converters
import com.example.dosezy.data.dao.MedicineDao
import com.example.dosezy.data.dao.ScheduleDao
import com.example.dosezy.data.dao.UserDao
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.User

@Database(
    entities = [User::class, Medicine::class, ScheduleEntry::class],
    version = 15,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DosezyDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun medicineDao(): MedicineDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: DosezyDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN currentStock INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE medicines ADD COLUMN refillThreshold INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE medicines ADD COLUMN autoDeductOnTake INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // User table additions
                db.execSQL("ALTER TABLE users ADD COLUMN allergies TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE users ADD COLUMN medicalConditions TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE users ADD COLUMN naggingRemindersEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN naggingIntervalMinutes INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE users ADD COLUMN naggingMaxRepeats INTEGER NOT NULL DEFAULT 3")

                // Medicine table additions
                db.execSQL("ALTER TABLE medicines ADD COLUMN notes TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE medicines ADD COLUMN pillShape TEXT NOT NULL DEFAULT 'ROUND'")
                db.execSQL("ALTER TABLE medicines ADD COLUMN pillColor TEXT NOT NULL DEFAULT '#1193D4'")
                db.execSQL("ALTER TABLE medicines ADD COLUMN startDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE medicines ADD COLUMN endDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE medicines ADD COLUMN durationDays INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN alarmSound TEXT NOT NULL DEFAULT 'SYSTEM_DEFAULT'")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN alarmDurationSeconds INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN customAlarmSoundPath TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE users ADD COLUMN customAlarmSoundTitle TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN allowDoseSkipping INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE schedule_entries ADD COLUMN skipReason TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN allowCustomDoseTime INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN hideAddMedicineNavButton INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN customDosages TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE schedule_entries ADD COLUMN dosage REAL DEFAULT NULL")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN allowDoseUndo INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN promptDoseNotes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE schedule_entries ADD COLUMN doseNotes TEXT DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): DosezyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DosezyDatabase::class.java,
                    "dosezy_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys = ON;")
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
