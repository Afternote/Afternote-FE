package com.afternote.feature.receiver.data.di

import javax.inject.Qualifier

/** Hilt 한정자: 본인 확인 완료 여부 전용 `DataStore<Preferences>` 바인딩 (다른 DataStore 와 구분). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IdentityVerificationDataStore
