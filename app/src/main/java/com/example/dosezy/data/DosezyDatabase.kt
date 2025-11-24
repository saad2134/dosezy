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
    version = 2,
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

        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Since we're adding new columns with default values, we can recreate the table
                // This is a simple approach for development
                database.execSQL("CREATE TABLE users_new (userId TEXT PRIMARY KEY NOT NULL, profilePicPath TEXT, fullName TEXT NOT NULL, age INTEGER NOT NULL, gender TEXT NOT NULL, contactNumber TEXT NOT NULL, isCurrentUser INTEGER NOT NULL, theme TEXT NOT NULL DEFAULT 'LIGHT', timeFormat TEXT NOT NULL DEFAULT 'HOUR_12', language TEXT NOT NULL DEFAULT 'ENGLISH', considerMissedAfter INTEGER NOT NULL DEFAULT 3)")

                // Copy data from old table to new table
                database.execSQL("INSERT INTO users_new (userId, profilePicPath, fullName, age, gender, contactNumber, isCurrentUser, theme, timeFormat, language, considerMissedAfter) SELECT userId, profilePicPath, fullName, age, gender, contactNumber, isCurrentUser, 'LIGHT', 'HOUR_12', 'ENGLISH', 3 FROM users")

                // Drop old table and rename new one
                database.execSQL("DROP TABLE users")
                database.execSQL("ALTER TABLE users_new RENAME TO users")
            }
        }

        fun getInstance(context: Context): DosezyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DosezyDatabase::class.java,
                    "dosezy_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}