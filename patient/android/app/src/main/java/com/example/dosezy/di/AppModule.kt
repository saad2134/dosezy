// AppModule.kt
package com.example.dosezy.di

import android.content.Context
import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.repository.MedicineRepository
import com.example.dosezy.data.repository.ScheduleRepository
import com.example.dosezy.data.repository.UserRepository
import com.example.dosezy.notifications.MedicineNotificationManager
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
        return DosezyDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideUserRepository(database: DosezyDatabase): UserRepository {
        return UserRepository(database)
    }

    @Provides
    @Singleton
    fun provideMedicineRepository(
        database: DosezyDatabase,
        scheduleRepository: ScheduleRepository,
        @ApplicationContext context: Context
    ): MedicineRepository {
        return MedicineRepository(database, scheduleRepository, context)
    }

    @Provides
    @Singleton
    fun provideScheduleRepository(database: DosezyDatabase): ScheduleRepository {
        return ScheduleRepository(database)
    }

    @Provides
    @Singleton
    fun provideMedicineNotificationManager(
        @ApplicationContext context: Context,
        database: DosezyDatabase
    ): MedicineNotificationManager {
        return MedicineNotificationManager(context, database)
    }
}