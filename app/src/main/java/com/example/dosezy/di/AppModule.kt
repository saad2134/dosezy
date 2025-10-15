package com.example.dosezy.di

import android.content.Context
import androidx.room.Room
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DosezyDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            DosezyDatabase::class.java,
            "dosezy_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideUserRepository(database: DosezyDatabase): UserRepository {
        return UserRepository(database)
    }

    @Provides
    @Singleton
    fun provideMedicineRepository(database: DosezyDatabase): MedicineRepository {
        return MedicineRepository(database)
    }

    @Provides
    @Singleton
    fun provideScheduleRepository(database: DosezyDatabase): ScheduleRepository {
        return ScheduleRepository(database)
    }
}