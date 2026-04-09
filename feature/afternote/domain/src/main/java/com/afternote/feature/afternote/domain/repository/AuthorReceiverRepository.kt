package com.afternote.feature.afternote.domain.repository

import com.afternote.feature.afternote.domain.model.author.AuthorReceiverDirectoryEntry
import kotlinx.coroutines.flow.Flow

/**
 * 작성자 본인의 userId 및 수신자 디렉터리(이름/관계 조회용) 접근.
 *
 * 디렉터리 목록의 SSOT는 데이터 레이어 구현의 [observeReceiversDirectory] / [currentReceiversDirectory] 입니다.
 * 로그아웃·계정 전환 시 [clearReceiversDirectory]로 비워 두면 UI 상태 오염을 막을 수 있습니다.
 */
interface AuthorReceiverRepository {
    fun currentAuthorUserId(): Long?

    fun observeReceiversDirectory(): Flow<List<AuthorReceiverDirectoryEntry>>

    fun currentReceiversDirectory(): List<AuthorReceiverDirectoryEntry>

    suspend fun refreshReceiversDirectory(): Result<Unit>

    suspend fun clearReceiversDirectory()
}
