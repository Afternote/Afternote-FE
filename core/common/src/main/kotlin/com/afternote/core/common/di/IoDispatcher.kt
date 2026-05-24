package com.afternote.core.common.di

import javax.inject.Qualifier

/** Hilt 한정자: I/O 작업 전용 [kotlinx.coroutines.CoroutineDispatcher] 바인딩. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
