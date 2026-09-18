package com.afternote.core.domain.testing

import com.afternote.core.domain.repository.PendingReceiverInvitationStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** [PendingReceiverInvitationStore] fake 정본 (#944). 토큰을 메모리 상태로 보관한다. */
class FakePendingReceiverInvitationStore(
    token: String? = null,
) : PendingReceiverInvitationStore {
    val tokenState = MutableStateFlow(token)

    @Volatile
    var clearCalls: Int = 0
        private set

    override val pendingToken: Flow<String?> get() = tokenState

    override suspend fun save(token: String) {
        tokenState.value = token
    }

    override suspend fun clear() {
        clearCalls += 1
        tokenState.value = null
    }
}
