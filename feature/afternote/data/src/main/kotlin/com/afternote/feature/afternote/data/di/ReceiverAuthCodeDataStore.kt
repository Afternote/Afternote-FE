package com.afternote.feature.afternote.data.di

import javax.inject.Qualifier

/** Hilt 한정자: 수신 인증 코드 전용 `DataStore<Preferences>` 바인딩 (토큰용 DataStore와 구분). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ReceiverAuthCodeDataStore
