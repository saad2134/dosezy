package com.example.dosezy.data.repository

import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class UserRepository(private val database: DosezyDatabase) {
    fun getAllUsers(): Flow<List<User>> = database.userDao().getAllUsers()

    // Add this method to get all users as list (not Flow)
    suspend fun getAllUsersList(): List<User> = database.userDao().getAllUsers().first()

    fun getUserById(userId: String): Flow<User?> = database.userDao().getUserById(userId)

    // Add this method to get user as object (not Flow)
    suspend fun getUserByIdSync(userId: String): User? = database.userDao().getUserById(userId).first()

    suspend fun insertUser(user: User) = database.userDao().insertUser(user)

    suspend fun updateUser(user: User) = database.userDao().updateUser(user)

    suspend fun deleteUser(user: User) = database.userDao().deleteUser(user)
}