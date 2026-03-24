package com.starosta.messenger.data.remote

import android.util.Log
import com.starosta.messenger.data.models.MessageWithDetails
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.OkHttpClient
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

sealed class SocketEvent {
    data class NewMessage(val message: MessageWithDetails) : SocketEvent()
    data class MessageEdited(val message: MessageWithDetails) : SocketEvent()
    data class MessageDeleted(val chatId: String, val messageId: String) : SocketEvent()
    data class MessagePinned(val chatId: String, val messageId: String) : SocketEvent()
    data class MessageUnpinned(val chatId: String, val messageId: String) : SocketEvent()
    data class MessageRead(val chatId: String, val userId: String, val messageIds: List<String>) : SocketEvent()
    data class TypingStart(val chatId: String, val userId: String, val userName: String) : SocketEvent()
    data class TypingStop(val chatId: String, val userId: String) : SocketEvent()
    data class UserStatus(val userId: String, val isOnline: Boolean) : SocketEvent()
    data class ChatNew(val chatId: String) : SocketEvent()
    object Connected : SocketEvent()
    object Disconnected : SocketEvent()
}

@Singleton
class WebSocketService @Inject constructor() {

    private var socket: Socket? = null
    private val _events = Channel<SocketEvent>(Channel.BUFFERED)
    val events: Flow<SocketEvent> = _events.receiveAsFlow()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun connect(wsUrl: String, token: String) {
        try {
            val options = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .setReconnection(true)
                .setReconnectionAttempts(Int.MAX_VALUE)
                .setReconnectionDelay(1000)
                .setReconnectionDelayMax(5000)
                .build()

            socket = IO.socket(wsUrl, options)

            socket?.apply {
                on(Socket.EVENT_CONNECT) {
                    _events.trySend(SocketEvent.Connected)
                    Log.d("WebSocket", "Connected")
                }

                on(Socket.EVENT_DISCONNECT) {
                    _events.trySend(SocketEvent.Disconnected)
                    Log.d("WebSocket", "Disconnected")
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    Log.e("WebSocket", "Connection error: ${args.firstOrNull()}")
                }

                on("message:new") { args ->
                    args.firstOrNull()?.let { data ->
                        try {
                            val message = json.decodeFromString<MessageWithDetails>(data.toString())
                            _events.trySend(SocketEvent.NewMessage(message))
                        } catch (e: Exception) {
                            Log.e("WebSocket", "Error parsing message:new", e)
                        }
                    }
                }

                on("message:edited") { args ->
                    args.firstOrNull()?.let { data ->
                        try {
                            val message = json.decodeFromString<MessageWithDetails>(data.toString())
                            _events.trySend(SocketEvent.MessageEdited(message))
                        } catch (e: Exception) {
                            Log.e("WebSocket", "Error parsing message:edited", e)
                        }
                    }
                }

                on("message:deleted") { args ->
                    args.firstOrNull()?.let { data ->
                        try {
                            val obj = JSONObject(data.toString())
                            _events.trySend(SocketEvent.MessageDeleted(
                                obj.getString("chatId"),
                                obj.getString("messageId")
                            ))
                        } catch (e: Exception) {
                            Log.e("WebSocket", "Error parsing message:deleted", e)
                        }
                    }
                }

                on("message:pinned") { args ->
                    args.firstOrNull()?.let { data ->
                        try {
                            val obj = JSONObject(data.toString())
                            _events.trySend(SocketEvent.MessagePinned(
                                obj.getString("chatId"),
                                obj.getString("messageId")
                            ))
                        } catch (e: Exception) {
                            Log.e("WebSocket", "Error parsing message:pinned", e)
                        }
                    }
                }

                on("message:unpinned") { args ->
                    args.firstOrNull()?.let { data ->
                        try {
                            val obj = JSONObject(data.toString())
                            _events.trySend(SocketEvent.MessageUnpinned(
                                obj.getString("chatId"),
                                obj.getString("messageId")
                            ))
                        } catch (e: Exception) {
                            Log.e("WebSocket", "Error parsing message:unpinned", e)
                        }
                    }
                }

                on("message:read") { args ->
                    args.firstOrNull()?.let { data ->
                        try {
                            val obj = JSONObject(data.toString())
                            val messageIds = obj.getJSONArray("messageIds").let { arr ->
                                (0 until arr.length()).map { arr.getString(it) }
                            }
                            _events.trySend(SocketEvent.MessageRead(
                                obj.getString("chatId"),
                                obj.getString("userId"),
                                messageIds
                            ))
                        } catch (e: Exception) {
                            Log.e("WebSocket", "Error parsing message:read", e)
                        }
                    }
                }

                on("typing:start") { args ->
                    args.firstOrNull()?.let { data ->
                        try {
                            val obj = JSONObject(data.toString())
                            _events.trySend(SocketEvent.TypingStart(
                                obj.getString("chatId"),
                                obj.getString("userId"),
                                obj.optString("userName", "Someone")
                            ))
                        } catch (e: Exception) {
                            Log.e("WebSocket", "Error parsing typing:start", e)
                        }
                    }
                }

                on("typing:stop") { args ->
                    args.firstOrNull()?.let { data ->
                        try {
                            val obj = JSONObject(data.toString())
                            _events.trySend(SocketEvent.TypingStop(
                                obj.getString("chatId"),
                                obj.getString("userId")
                            ))
                        } catch (e: Exception) {
                            Log.e("WebSocket", "Error parsing typing:stop", e)
                        }
                    }
                }

                on("user:status") { args ->
                    args.firstOrNull()?.let { data ->
                        try {
                            val obj = JSONObject(data.toString())
                            _events.trySend(SocketEvent.UserStatus(
                                obj.getString("userId"),
                                obj.getBoolean("isOnline")
                            ))
                        } catch (e: Exception) {
                            Log.e("WebSocket", "Error parsing user:status", e)
                        }
                    }
                }

                connect()
            }
        } catch (e: Exception) {
            Log.e("WebSocket", "Connection error", e)
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    fun joinChat(chatId: String) {
        socket?.emit("chat:join", chatId)
    }

    fun leaveChat(chatId: String) {
        socket?.emit("chat:leave", chatId)
    }

    fun sendTypingStart(chatId: String) {
        socket?.emit("typing:start", JSONObject().put("chatId", chatId))
    }

    fun sendTypingStop(chatId: String) {
        socket?.emit("typing:stop", JSONObject().put("chatId", chatId))
    }

    fun markMessagesRead(chatId: String, messageIds: List<String>) {
        val obj = JSONObject()
            .put("chatId", chatId)
            .put("messageIds", messageIds)
        socket?.emit("messages:read", obj)
    }

    fun isConnected(): Boolean = socket?.connected() == true
}
