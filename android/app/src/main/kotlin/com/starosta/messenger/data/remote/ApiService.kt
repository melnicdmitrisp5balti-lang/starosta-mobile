package com.starosta.messenger.data.remote

import com.starosta.messenger.data.models.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/auth/google")
    suspend fun googleSignIn(@Body request: GoogleAuthRequest): Response<AuthResponse>

    @POST("api/auth/phone/send-otp")
    suspend fun sendOtp(@Body request: PhoneOtpRequest): Response<ApiResponse<Unit>>

    @POST("api/auth/phone/verify")
    suspend fun verifyOtp(@Body request: PhoneVerifyRequest): Response<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Body request: Map<String, String>): Response<ApiResponse<Unit>>

    @POST("api/auth/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): Response<ApiResponse<Unit>>

    // Users
    @GET("api/users/me")
    suspend fun getMe(): Response<User>

    @PATCH("api/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<User>

    @PATCH("api/users/me/avatar")
    suspend fun updateAvatar(@Body request: UpdateAvatarRequest): Response<User>

    @GET("api/users/search")
    suspend fun searchUsers(@Query("q") query: String): Response<List<User>>

    @GET("api/users/{userId}")
    suspend fun getUserById(@Path("userId") userId: String): Response<User>

    // Chats
    @GET("api/chats")
    suspend fun getChats(): Response<List<ChatWithDetails>>

    @GET("api/chats/{chatId}")
    suspend fun getChatById(@Path("chatId") chatId: String): Response<ChatWithDetails>

    @POST("api/chats/private")
    suspend fun createPrivateChat(@Body request: CreatePrivateChatRequest): Response<ChatWithDetails>

    @POST("api/chats/group")
    suspend fun createGroupChat(@Body request: CreateGroupChatRequest): Response<ChatWithDetails>

    @PATCH("api/chats/{chatId}")
    suspend fun updateChat(
        @Path("chatId") chatId: String,
        @Body request: UpdateChatRequest
    ): Response<ChatWithDetails>

    @POST("api/chats/{chatId}/members")
    suspend fun addMember(
        @Path("chatId") chatId: String,
        @Body request: AddMemberRequest
    ): Response<ApiResponse<Unit>>

    @DELETE("api/chats/{chatId}/members/{userId}")
    suspend fun removeMember(
        @Path("chatId") chatId: String,
        @Path("userId") userId: String
    ): Response<ApiResponse<Unit>>

    @DELETE("api/chats/{chatId}")
    suspend fun leaveChat(@Path("chatId") chatId: String): Response<ApiResponse<Unit>>

    // Messages
    @GET("api/messages/{chatId}")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<MessagesResponse>

    @POST("api/messages/{chatId}")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body request: SendMessageRequest
    ): Response<MessageWithDetails>

    @PATCH("api/messages/{chatId}/{messageId}")
    suspend fun editMessage(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String,
        @Body request: EditMessageRequest
    ): Response<MessageWithDetails>

    @DELETE("api/messages/{chatId}/{messageId}")
    suspend fun deleteMessage(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String
    ): Response<ApiResponse<Unit>>

    @POST("api/messages/{chatId}/{messageId}/pin")
    suspend fun pinMessage(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String
    ): Response<PinnedMessage>

    @DELETE("api/messages/{chatId}/{messageId}/pin")
    suspend fun unpinMessage(
        @Path("chatId") chatId: String,
        @Path("messageId") messageId: String
    ): Response<ApiResponse<Unit>>

    @POST("api/messages/{chatId}/read")
    suspend fun markMessagesRead(
        @Path("chatId") chatId: String,
        @Body request: MarkReadRequest
    ): Response<ApiResponse<Unit>>

    // Files
    @Multipart
    @POST("api/files/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<FileUploadResponse>

    @Multipart
    @POST("api/files/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<ApiResponse<String>>
}
