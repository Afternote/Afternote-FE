package com.afternote.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.afternote.core.datastore.di.UserProfileDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 프로필 캐시 로컬 저장소. 현재는 이름 한 필드만 보관한다.
 * 홈 진입 시 GET /users/me 응답이 도착하기 전 placeholder로 즉시 노출하기 위함이다.
 *
 * 읽기 스트림은 [IOException] 시 [emptyPreferences]로 복구한다.
 */
@Singleton
class UserProfileDataSource
    @Inject
    constructor(
        @param:UserProfileDataStore private val dataStore: DataStore<Preferences>,
    ) {
        private object Keys {
            val USER_NAME = stringPreferencesKey("user_name")
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

        suspend fun getCachedUserName(): String? = preferencesFlow.first()[Keys.USER_NAME]

        suspend fun saveUserName(name: String) {
            dataStore.edit { prefs ->
                prefs[Keys.USER_NAME] = name
            }
        }

        suspend fun clear() {
            dataStore.edit { prefs ->
                prefs.clear()
            }
        }
    }
