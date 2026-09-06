package com.afternote.feature.receiver.data.di

import javax.inject.Qualifier

/** Hilt 한정자: 발신자별 본인 확인 완료 상태 전용 `DataStore<Preferences>` 바인딩. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IdentityVerificationDataStore
