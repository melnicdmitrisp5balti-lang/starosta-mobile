package com.starosta.messenger.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.starosta.messenger.data.local.UserDao
import com.starosta.messenger.data.models.*
import com.starosta.messenger.data.remote.ApiService
import com.starosta.messenger.data.remote.AuthInterceptor
import com.starosta.messenger.data.remote.WebSocketService
import com.starosta.messenger.data.remote.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val webSocketService: WebSocketService,
    @ApplicationContext private val context: Context
) {
    companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        val USER_ID_KEY = stringPreferencesKey("user_id")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN_KEY] }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID_KEY] }
    val isLoggedIn: Flow<Boolean> = accessToken.map { it != null }

    suspend fun register(email: String, password: String, name: String): Result<AuthResponse> {
        return try {
            val response = apiService.register(RegisterRequest(email, password, name))
            if (response.isSuccessful) {
                val body = response.body()!!
                saveAuth(body)
                Result.Success(body)
            } else {
                Result.Error(parseError(response.errorBody()?.string()), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = apiService.login(AuthRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                saveAuth(body)
                Result.Success(body)
            } else {
                Result.Error(parseError(response.errorBody()?.string()), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun googleSignIn(idToken: String): Result<AuthResponse> {
        return try {
            val response = apiService.googleSignIn(GoogleAuthRequest(idToken))
            if (response.isSuccessful) {
                val body = response.body()!!
                saveAuth(body)
                Result.Success(body)
            } else {
                Result.Error(parseError(response.errorBody()?.string()), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun sendOtp(phone: String): Result<Unit> {
        return try {
            val response = apiService.sendOtp(PhoneOtpRequest(phone))
            if (response.isSuccessful) Result.Success(Unit)
            else Result.Error(parseError(response.errorBody()?.string()), response.code())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun verifyOtp(phone: String, code: String, name: String?): Result<AuthResponse> {
        return try {
            val response = apiService.verifyOtp(PhoneVerifyRequest(phone, code, name))
            if (response.isSuccessful) {
                val body = response.body()!!
                saveAuth(body)
                Result.Success(body)
            } else {
                Result.Error(parseError(response.errorBody()?.string()), response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun logout() {
        try {
            val refreshToken = context.dataStore.data.map { it[REFRESH_TOKEN_KEY] }.let {
                var value: String? = null
                it.collect { v -> value = v; return@collect }
                value
            }
            if (refreshToken != null) {
                apiService.logout(mapOf("refreshToken" to refreshToken))
            }
        } catch (_: Exception) {}

        webSocketService.disconnect()
        clearAuth()
        userDao.deleteAllUsers()
    }

    suspend fun updateFcmToken(token: String) {
        try {
            apiService.updateFcmToken(FcmTokenRequest(token))
        } catch (_: Exception) {}
    }

    private suspend fun saveAuth(auth: AuthResponse) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = auth.accessToken
            prefs[REFRESH_TOKEN_KEY] = auth.refreshToken
            prefs[USER_ID_KEY] = auth.user.id
        }
        userDao.insertUser(auth.user)
    }

    private suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
        }
    }

    private fun parseError(body: String?): String {
        return try {
            kotlinx.serialization.json.Json.parseToJsonElement(body ?: "")
                .jsonObject["error"]?.toString()?.trim('"') ?: "Unknown error"
        } catch (_: Exception) {
            body ?: "Unknown error"
        }
    }
}
