package com.afternote.core.datastore.di

import javax.inject.Qualifier

/** Hilt 한정자: 사용자 프로필 캐시 전용 [androidx.datastore.core.DataStore] 바인딩. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserProfileDataStore
