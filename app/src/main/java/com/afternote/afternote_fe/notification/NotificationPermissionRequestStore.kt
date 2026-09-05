package com.afternote.afternote_fe.notification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.afternote.afternote_fe.notification.di.NotificationPermissionDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `POST_NOTIFICATIONS` 런타임 권한을 사용자에게 한 번이라도 물었는지 기록한다 (#1454).
 *
 * 시스템은 "이미 물어봤는지" 를 알려주지 않는다 — `checkSelfPermission` 은 현재 상태만,
 * `shouldShowRequestPermissionRationale` 은 "아직 안 물음" 과 "영구 거부" 를 같은 false 로 돌려준다.
 * 그래서 요청 1회 보장은 앱이 직접 기록해야 한다.
 *
 * 수명은 [com.afternote.core.datastore.StoreScope.DEVICE] — 권한은 계정이 아니라 기기에 붙으므로
 * 로그아웃으로 지워지면 다음 로그인마다 다시 묻게 된다.
 */
interface NotificationPermissionRequestStore {
    /** 권한 다이얼로그를 한 번이라도 띄웠으면 true. */
    val hasRequested: Flow<Boolean>

    suspend fun markRequested()
}

@Singleton
class DataStoreNotificationPermissionRequestStore
    @Inject
    constructor(
        @param:NotificationPermissionDataStore private val dataStore: DataStore<Preferences>,
    ) : NotificationPermissionRequestStore {
        override val hasRequested: Flow<Boolean> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs -> prefs[HAS_REQUESTED] ?: false }

        override suspend fun markRequested() {
            dataStore.edit { prefs ->
                prefs[HAS_REQUESTED] = true
            }
        }

        private companion object {
            val HAS_REQUESTED = booleanPreferencesKey("has_requested_post_notifications")
        }
    }
