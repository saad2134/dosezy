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
    version = 8,
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

        fun getInstance(context: Context): DosezyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DosezyDatabase::class.java,
                    "dosezy_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
