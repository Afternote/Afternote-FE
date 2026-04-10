package com.afternote.feature.afternote.data.network

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug 전용 Mock 모드 on/off 상태 저장.
 * OkHttp 인터셉터에서 동기적으로 읽어야 하므로 DataStore 대신 SharedPreferences 사용.
 */
@Singleton
class MockModePreferences
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Data Layer(Interceptor)용: 동기적 접근
        var isEnabled: Boolean
            get() = prefs.getBoolean(KEY_MOCK_ENABLED, true)
            set(value) {
                prefs.edit().putBoolean(KEY_MOCK_ENABLED, value).apply()
            }

        // UI Layer(ViewModel)용: 단방향 데이터 흐름을 위한 스트림
        val isEnabledFlow: Flow<Boolean> =
            callbackFlow {
                trySend(isEnabled)

                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                        if (key == KEY_MOCK_ENABLED) {
                            trySend(isEnabled)
                        }
                    }

                prefs.registerOnSharedPreferenceChangeListener(listener)
                awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

        private companion object {
            const val PREFS_NAME = "debug_mock_mode"
            const val KEY_MOCK_ENABLED = "mock_enabled"
        }
    }
