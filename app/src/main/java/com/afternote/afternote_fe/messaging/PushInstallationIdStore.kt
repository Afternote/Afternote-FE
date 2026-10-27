package com.afternote.afternote_fe.messaging

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.afternote.afternote_fe.messaging.di.PushInstallationDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 이 앱 설치를 가리키는 UUID 를 한 번 만들어 보관한다 (#2321).
 *
 * 서버는 이 값으로 같은 설치의 권한 보고(`push-installations`)와 토큰 등록(`push-tokens`)을 묶는다.
 * FID 는 UUID 형식이 아니고 회전도 하므로 그 자리를 대신하지 못한다.
 *
 * 수명은 [com.afternote.core.datastore.StoreScope.DEVICE] 다. 계정이 아니라 설치에 붙는 값이라
 * 로그아웃으로 지워지면 같은 기기가 서버에 새 설치로 쌓인다. 백업에서는 빠진다 — `datastore/`
 * 디렉터리 전체가 백업 제외라(#1876) 새 기기로 복원돼도 옛 설치를 사칭하지 않는다.
 */
interface PushInstallationIdStore {
    suspend fun installationId(): String
}

@Singleton
class DataStorePushInstallationIdStore
    @Inject
    constructor(
        @param:PushInstallationDataStore private val dataStore: DataStore<Preferences>,
    ) : PushInstallationIdStore {
        override suspend fun installationId(): String {
            dataStore.data.first()[INSTALLATION_ID]?.let { return it }
            // edit 은 원자적이라 두 호출이 동시에 들어와도 먼저 쓴 값 하나만 남는다.
            return dataStore
                .edit { prefs ->
                    if (prefs[INSTALLATION_ID] == null) prefs[INSTALLATION_ID] = UUID.randomUUID().toString()
                }[INSTALLATION_ID]
                .let(::checkNotNull)
        }

        private companion object {
            val INSTALLATION_ID = stringPreferencesKey("installation_id")
        }
    }
