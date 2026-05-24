package com.afternote.core.datastore.di

import javax.inject.Qualifier

/** Hilt 한정자: 토큰 저장 전용 [androidx.datastore.core.DataStore] 바인딩. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TokenDataStore
