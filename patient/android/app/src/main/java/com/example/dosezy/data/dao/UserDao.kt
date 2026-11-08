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

    @Query("SELECT * FROM users")
    suspend fun getAllUsersDirect(): List<User>

    // Guard: Atomically update isCurrentUser flag in a single SQLite statement to prevent Room intermediate emissions with 0 active users triggering auto-promotion
    @Query("UPDATE users SET isCurrentUser = CASE WHEN userId = :userId THEN 1 ELSE 0 END")
    suspend fun setCurrentUserAtomic(userId: String)
}