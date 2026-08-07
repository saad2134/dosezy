package com.example.dosezy.data.dao

import androidx.room.*
import com.example.dosezy.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserById(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserByIdDirect(userId: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUserById(userId: String)
}