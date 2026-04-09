package com.afternote.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * 액세스·리프레시 토큰 및 userId 저장용 Preferences DataStore.
 * [TokenManager]와 [TokenDataStoreModule]에서 동일 인스턴스를 참조합니다.
 */
internal val Context.tokenPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "Token",
)
