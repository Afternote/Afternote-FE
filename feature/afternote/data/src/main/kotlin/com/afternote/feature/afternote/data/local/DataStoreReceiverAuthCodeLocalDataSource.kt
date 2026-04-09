package com.afternote.feature.afternote.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.afternote.feature.afternote.data.di.ReceiverAuthCodeDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private object ReceiverAuthCodeKeys {
    val AUTH_CODE = stringPreferencesKey("receiver_auth_code")
}

/**
 * [ReceiverAuthCodeLocalDataSource]의 DataStore(Preferences) 구현.
 *
 * 인증 코드는 민감할 수 있어, 필요 시 Keystore·암호화 저장소로 교체하는 것을 검토하세요.
 */
@Singleton
class DataStoreReceiverAuthCodeLocalDataSource
    @Inject
    constructor(
        @ReceiverAuthCodeDataStore private val dataStore: DataStore<Preferences>,
    ) : ReceiverAuthCodeLocalDataSource {
        override val savedCodeFlow: Flow<String?> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { preferences ->
                    preferences[ReceiverAuthCodeKeys.AUTH_CODE]?.takeIf { it.isNotBlank() }
                }

        override suspend fun saveCode(code: String) {
            val trimmed = code.trim()
            dataStore.edit { preferences ->
                if (trimmed.isEmpty()) {
                    preferences.remove(ReceiverAuthCodeKeys.AUTH_CODE)
                } else {
                    preferences[ReceiverAuthCodeKeys.AUTH_CODE] = trimmed
                }
            }
        }

        override suspend fun clearCode() {
            dataStore.edit { preferences ->
                preferences.remove(ReceiverAuthCodeKeys.AUTH_CODE)
            }
        }
    }
