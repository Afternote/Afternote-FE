package com.afternote.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.afternote.core.datastore.di.TokenDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore 기반 액세스·리프레시 토큰 및 userId 로컬 저장.
 * 읽기 스트림은 [IOException] 시 [emptyPreferences]로 복구합니다.
 */
@Singleton
class TokenDataSource
    @Inject
    constructor(
        @param:TokenDataStore private val dataStore: DataStore<Preferences>,
    ) {
        private object Keys {
            val ACCESS_TOKEN = stringPreferencesKey("access_token")
            val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
            val USER_ID = longPreferencesKey("user_id")
        }

        private val preferencesFlow: Flow<Preferences> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }

        val isLoggedIn: Flow<Boolean> =
            preferencesFlow.map { prefs ->
                prefs[Keys.ACCESS_TOKEN] != null
            }

        suspend fun saveTokens(
            accessToken: String,
            refreshToken: String,
            userId: Long,
        ) {
            dataStore.edit { prefs ->
                prefs[Keys.ACCESS_TOKEN] = accessToken
                prefs[Keys.REFRESH_TOKEN] = refreshToken
                prefs[Keys.USER_ID] = userId
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
                prefs[Keys.ACCESS_TOKEN] = accessToken
                prefs[Keys.REFRESH_TOKEN] = refreshToken
            }
        }

        suspend fun getAccessToken(): String? = preferencesFlow.first()[Keys.ACCESS_TOKEN]

        suspend fun getRefreshToken(): String? = preferencesFlow.first()[Keys.REFRESH_TOKEN]

        suspend fun getUserId(): Long? = preferencesFlow.first()[Keys.USER_ID]
    }
