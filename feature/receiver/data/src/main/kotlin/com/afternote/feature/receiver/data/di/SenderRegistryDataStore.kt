package com.afternote.feature.receiver.data.di

import javax.inject.Qualifier

/** Hilt 한정자: 발신자 카드 레지스트리 전용 `DataStore<Preferences>` 바인딩. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SenderRegistryDataStore
