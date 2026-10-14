package com.afternote.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.afternote.core.datastore.di.ReceiverInvitationDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 대기 중인 카카오톡 수신자 초대 토큰 로컬 저장 (#944).
 *
 * 값은 비밀이라 로그에 남기지 않는다. 읽기 스트림은 [IOException] 시 [emptyPreferences] 로 복구한다.
 */
@Singleton
class ReceiverInvitationDataSource
    @Inject
    constructor(
        @param:ReceiverInvitationDataStore private val dataStore: DataStore<Preferences>,
    ) {
        private object Keys {
            val PENDING_TOKEN = stringPreferencesKey("pending_invitation_token")
        }

        val pendingTokenFlow: Flow<String?> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs -> prefs[Keys.PENDING_TOKEN]?.takeIf { it.isNotBlank() } }

        suspend fun savePendingToken(token: String) {
            dataStore.edit { prefs -> prefs[Keys.PENDING_TOKEN] = token }
        }

        suspend fun clearPendingToken() {
            dataStore.edit { prefs -> prefs.remove(Keys.PENDING_TOKEN) }
        }
    }
