package com.afternote.core.datastore.di

import javax.inject.Qualifier

/** Hilt 한정자: 대기 중인 카카오톡 수신자 초대 토큰 전용 [androidx.datastore.core.DataStore] 바인딩 (#944). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ReceiverInvitationDataStore
