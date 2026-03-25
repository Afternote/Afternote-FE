package com.afternote.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
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

        suspend fun saveAccessToken(
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

        suspend fun getAccessToken(): String? = getPrefs()[accessTokenKey]

        suspend fun getRefreshToken(): String? = getPrefs()[refreshTokenKey]

        suspend fun getUserId(): Long? = getPrefs()[userIdKey]
    }
