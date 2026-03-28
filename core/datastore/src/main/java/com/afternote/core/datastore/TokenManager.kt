package com.afternote.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("Token")

@Singleton
class TokenManager
    @Inject
    constructor(
        @param:ApplicationContext val context: Context,
    ) {
        private val accessTokenKey = stringPreferencesKey("access_token")
        private val refreshTokenKey = stringPreferencesKey("refresh_token")
        private val userIdKey = longPreferencesKey("user_id")

        private val dataStore get() = context.dataStore
        private val dataFlow get() = dataStore.data

        private suspend fun getPrefs() = dataFlow.first()

        val isLoggedInFlow: Flow<Boolean> =
            dataFlow.map { prefs ->
                prefs[accessTokenKey] != null
            }

        suspend fun saveTokens(
            accessToken: String,
            refreshToken: String,
            userId: Long,
        ) {
            dataStore.edit { prefs ->
                prefs[accessTokenKey] = accessToken
                prefs[refreshTokenKey] = refreshToken
                prefs[userIdKey] = userId
            }
        }

        suspend fun clearTokens() {
            dataStore.edit { prefs ->
                prefs.clear()
            }
        }

        suspend fun updateTokens(
            accessToken: String,
            refreshToken: String,
        ) {
            dataStore.edit { prefs ->
                prefs[accessTokenKey] = accessToken
                prefs[refreshTokenKey] = refreshToken
            }
        }

        suspend fun getAccessToken(): String? = getPrefs()[accessTokenKey]

        suspend fun getRefreshToken(): String? = getPrefs()[refreshTokenKey]
    }
