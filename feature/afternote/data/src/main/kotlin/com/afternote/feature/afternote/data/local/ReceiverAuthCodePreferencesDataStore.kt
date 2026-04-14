package com.afternote.feature.afternote.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * 수신 인증 코드용 Preferences DataStore.
 * 기존 SharedPreferences XML(`afternote_receiver_auth`)과는 별도 파일이며 자동 마이그레이션은 하지 않습니다.
 */
internal val Context.receiverAuthCodePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "afternote_receiver_auth",
)
