package com.afternote.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * 사용자 프로필 캐시용 Preferences DataStore.
 *
 * 프로필 정보(이름 등)는 거의 변하지 않으므로, 콜드스타트 시 GET /users/me RTT(약 1.3초)를
 * 기다리지 않고 즉시 표시할 수 있도록 마지막 응답을 디스크에 보관한다.
 *
 * [com.afternote.core.datastore.UserProfileDataSource]와
 * [com.afternote.core.datastore.di.UserProfileDataStoreModule]에서 동일 인스턴스를 참조한다.
 */
internal val Context.userProfilePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "UserProfile",
)
