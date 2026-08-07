package com.example.dosezy.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.dosezy.data.converters.Converters
import com.example.dosezy.data.dao.MedicineDao
import com.example.dosezy.data.dao.ScheduleDao
import com.example.dosezy.data.dao.UserDao
import com.example.dosezy.data.model.Medicine
import com.example.dosezy.data.model.ScheduleEntry
import com.example.dosezy.data.model.User

@Database(
    entities = [User::class, Medicine::class, ScheduleEntry::class],
    version = 4,
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

        fun getInstance(context: Context): DosezyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DosezyDatabase::class.java,
                    "dosezy_database"
                )
                    .fallbackToDestructiveMigration() // clear database on version change
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}