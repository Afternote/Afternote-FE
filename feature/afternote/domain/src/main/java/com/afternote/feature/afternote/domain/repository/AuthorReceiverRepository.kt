package com.afternote.feature.afternote.domain.repository

import com.afternote.feature.afternote.domain.model.author.AuthorReceiverEntry
import kotlinx.coroutines.flow.Flow

/**
 * 작성자 본인의 userId 및 수신자 목록(이름/관계 조회용) 접근.
 *
 * 수신자 목록의 SSOT는 데이터 레이어 구현의 [observeReceivers] / [currentReceivers] 입니다.
 * 로그아웃·계정 전환 시 [clearReceivers]로 비워 두면 UI 상태 오염을 막을 수 있습니다.
 */
interface AuthorReceiverRepository {
    fun currentAuthorUserId(): Long?

    fun observeReceivers(): Flow<List<AuthorReceiverEntry>>

    fun currentReceivers(): List<AuthorReceiverEntry>

    suspend fun refreshReceivers(): Result<Unit>

    suspend fun clearReceivers()
}
