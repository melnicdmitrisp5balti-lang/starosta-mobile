package com.starosta.messenger.repository

import com.starosta.messenger.data.local.UserDao
import com.starosta.messenger.data.models.*
import com.starosta.messenger.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao
) {
    fun getLocalUser(userId: String): Flow<User?> = userDao.getUserById(userId)

    suspend fun getMe(): Result<User> {
        return try {
            val response = apiService.getMe()
            if (response.isSuccessful) {
                val user = response.body()!!
                userDao.insertUser(user)
                Result.Success(user)
            } else {
                Result.Error("Failed to load profile", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateProfile(name: String?, username: String?, status: String?): Result<User> {
        return try {
            val response = apiService.updateProfile(UpdateProfileRequest(name, username, status))
            if (response.isSuccessful) {
                val user = response.body()!!
                userDao.insertUser(user)
                Result.Success(user)
            } else {
                Result.Error("Failed to update profile", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val response = apiService.searchUsers(query)
            if (response.isSuccessful) {
                val users = response.body()!!
                userDao.insertUsers(users)
                Result.Success(users)
            } else {
                Result.Error("Search failed", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getUserById(userId: String): Result<User> {
        return try {
            val response = apiService.getUserById(userId)
            if (response.isSuccessful) {
                val user = response.body()!!
                userDao.insertUser(user)
                Result.Success(user)
            } else {
                Result.Error("User not found", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
