package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.feature.afternote.domain.model.author.AuthorReceiverEntry
import com.afternote.feature.afternote.domain.repository.AuthorReceiverRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 작성자 수신자 목록 SSOT. API 연동 시 [refreshReceivers]에서 원격을 받아 갱신합니다.
 */
@Singleton
class AuthorReceiverRepositoryImpl
    @Inject
    constructor() : AuthorReceiverRepository {
        private val receivers =
            MutableStateFlow<List<AuthorReceiverEntry>>(emptyList())

        override fun observeReceivers(): Flow<List<AuthorReceiverEntry>> = receivers.asStateFlow()

        override fun currentReceivers(): List<AuthorReceiverEntry> = receivers.value

        // TODO: AuthDataSource 등 사용자 정보 의존성 주입 후 실제 값으로 교체
        override fun currentAuthorUserId(): Long? = null

        override suspend fun refreshReceivers(): Result<Unit> {
            val userId =
                currentAuthorUserId() ?: run {
                    clearReceivers()
                    return Result.failure(IllegalStateException("No current author user id"))
                }
            return fetchReceiversForUser(userId)
                .onSuccess { entries -> receivers.update { entries } }
                .map { }
        }

        override suspend fun clearReceivers() {
            receivers.update { emptyList() }
        }

        private suspend fun fetchReceiversForUser(userId: Long): Result<List<AuthorReceiverEntry>> = Result.success(emptyList())
    }
