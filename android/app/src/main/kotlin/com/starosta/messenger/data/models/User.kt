package com.starosta.messenger.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    val email: String? = null,
    val phone: String? = null,
    val username: String,
    val name: String,
    val avatarUrl: String? = null,
    val status: String? = "Hey there! I am using Starosta Messenger",
    val isOnline: Boolean = false,
    val lastSeen: String? = null,
    val isVerified: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class AuthRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

@Serializable
data class GoogleAuthRequest(
    val idToken: String
)

@Serializable
data class PhoneOtpRequest(
    val phone: String
)

@Serializable
data class PhoneVerifyRequest(
    val phone: String,
    val code: String,
    val name: String? = null
)

@Serializable
data class AuthResponse(
    val user: User,
    val accessToken: String,
    val refreshToken: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String
)

@Serializable
data class FcmTokenRequest(
    val fcmToken: String
)

@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val username: String? = null,
    val status: String? = null
)

@Serializable
data class UpdateAvatarRequest(
    val avatarUrl: String
)
