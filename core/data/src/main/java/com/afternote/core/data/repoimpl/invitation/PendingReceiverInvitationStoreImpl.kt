package com.afternote.core.data.repoimpl.invitation

import com.afternote.core.datastore.ReceiverInvitationDataSource
import com.afternote.core.domain.repository.PendingReceiverInvitationStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** [PendingReceiverInvitationStore] 구현 — 기기 수명 DataStore 에 위임한다 (#944). */
internal class PendingReceiverInvitationStoreImpl
    @Inject
    constructor(
        private val dataSource: ReceiverInvitationDataSource,
    ) : PendingReceiverInvitationStore {
        override val pendingToken: Flow<String?> get() = dataSource.pendingTokenFlow

        override suspend fun save(token: String) = dataSource.savePendingToken(token)

        override suspend fun clear() = dataSource.clearPendingToken()
    }
