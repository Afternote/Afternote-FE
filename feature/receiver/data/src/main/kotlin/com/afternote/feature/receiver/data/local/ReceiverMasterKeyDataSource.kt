package com.afternote.feature.receiver.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.afternote.feature.receiver.data.di.ReceiverMasterKeyDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private object ReceiverMasterKeyKeys {
    val MASTER_KEY = stringPreferencesKey("receiver_auth_code")
}

/**
 * 수신 인증 코드 로컬 저장 (DataStore Preferences).
 * 도메인 [com.afternote.feature.receiver.domain.repository.ReceiverRepository]는 구현체에서만 이 클래스를 사용합니다.
 */
@Singleton
class ReceiverMasterKeyDataSource
    @Inject
    constructor(
        @param:ReceiverMasterKeyDataStore private val dataStore: DataStore<Preferences>,
    ) {
        val savedCodeFlow: Flow<String?> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { preferences ->
                    preferences[ReceiverMasterKeyKeys.MASTER_KEY]?.takeIf { it.isNotBlank() }
                }

        suspend fun saveCode(code: String) {
            val trimmed = code.trim()
            dataStore.edit { preferences ->
                if (trimmed.isEmpty()) {
                    preferences.remove(ReceiverMasterKeyKeys.MASTER_KEY)
                } else {
                    preferences[ReceiverMasterKeyKeys.MASTER_KEY] = trimmed
                }
            }
        }
    }
