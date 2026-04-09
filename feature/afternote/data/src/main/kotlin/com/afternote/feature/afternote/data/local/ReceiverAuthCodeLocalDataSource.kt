package com.afternote.feature.afternote.data.local

import kotlinx.coroutines.flow.Flow

/**
 * 수신자 인증 코드 로컬 저장소 (DataStore 등).
 *
 * [savedCodeFlow]로 반응형 구독이 가능하고, 쓰기는 suspend로 디스크 I/O를 메인 스레드에서 분리합니다.
 *
 * 구현: [DataStoreReceiverAuthCodeLocalDataSource] — Hilt는
 * [com.afternote.feature.afternote.data.di.AfternoteReceiverAuthLocalModule]에서 바인딩합니다.
 */
interface ReceiverAuthCodeLocalDataSource {
    val savedCodeFlow: Flow<String?>

    suspend fun saveCode(code: String)

    suspend fun clearCode()
}
