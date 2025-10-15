package com.example.dosezy.data.repository

import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val database: DosezyDatabase) {
    fun getAllUsers(): Flow<List<User>> = database.userDao().getAllUsers()

    fun getUserById(userId: String): Flow<User?> = database.userDao().getUserById(userId)

    suspend fun insertUser(user: User) = database.userDao().insertUser(user)

    suspend fun updateUser(user: User) = database.userDao().updateUser(user)

    suspend fun deleteUser(user: User) = database.userDao().deleteUser(user)
}