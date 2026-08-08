package com.example.dosezy.data.repository

import com.example.dosezy.data.DosezyDatabase
import com.example.dosezy.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class UserRepository(private val database: DosezyDatabase) {
    fun getAllUsers(): Flow<List<User>> = database.userDao().getAllUsers()

    suspend fun getAllUsersList(): List<User> =
        withContext(Dispatchers.IO) {
            try {
                database.userDao().getAllUsers().first()
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "Error getting users list", e)
                emptyList()
            }
        }

    fun getUserById(userId: String): Flow<User?> = database.userDao().getUserById(userId)

    suspend fun getUserByIdSync(userId: String): User? =
        withContext(Dispatchers.IO) {
            try {
                database.userDao().getUserById(userId).first()
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "Error getting user by ID", e)
                null
            }
        }

    suspend fun insertUser(user: User) =
        withContext(Dispatchers.IO) {
            try {
                database.userDao().insertUser(user)
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "Error inserting user", e)
                throw e
            }
        }

    suspend fun updateUser(user: User) =
        withContext(Dispatchers.IO) {
            try {
                database.userDao().updateUser(user)
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "Error updating user", e)
                throw e
            }
        }

    suspend fun deleteUser(user: User) =
        withContext(Dispatchers.IO) {
            try {
                database.userDao().deleteUser(user)
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "Error deleting user", e)
                throw e
            }
        }
}