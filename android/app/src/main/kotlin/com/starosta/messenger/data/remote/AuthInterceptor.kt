package com.starosta.messenger.data.remote

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore("auth_prefs")

@Singleton
class AuthInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {

    companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            context.dataStore.data.map { prefs ->
                prefs[ACCESS_TOKEN_KEY]
            }.first()
        }

        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}
