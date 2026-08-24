package com.afternote.feature.receiver.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * 본인 확인 완료 여부 영구 저장용 DataStore.
 * 발신자별로 분리하지 않는 사람 단위 1회 검증 정책 — 단일 boolean 키.
 */
internal val Context.identityVerificationPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "afternote_identity_verification",
)
