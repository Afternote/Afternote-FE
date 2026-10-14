package com.afternote.core.domain.testing

import com.afternote.core.domain.repository.ReceiverInvitationRepository
import com.afternote.core.model.user.ReceiverInvitationAccepted
import com.afternote.core.model.user.ReceiverInvitationCreated
import com.afternote.core.model.user.ReceiverInvitationLookup
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [ReceiverInvitationRepository] fake 정본 (#944).
 *
 * 기본은 성공을 돌려주고 호출 인자를 기록한다. 실패 경로는 `onX` 로 갈아끼운다.
 */
class FakeReceiverInvitationRepository(
    @Volatile var created: ReceiverInvitationCreated = ReceiverInvitationCreated(DEFAULT_TOKEN),
    @Volatile var lookup: ReceiverInvitationLookup = ReceiverInvitationLookup(inviterName = DEFAULT_INVITER, isExpired = false),
    @Volatile var accepted: ReceiverInvitationAccepted = ReceiverInvitationAccepted(inviterName = DEFAULT_INVITER),
    var onCreate: (suspend () -> Result<ReceiverInvitationCreated>)? = null,
    var onLookup: (suspend (String) -> Result<ReceiverInvitationLookup>)? = null,
    var onAccept: (suspend (String) -> Result<ReceiverInvitationAccepted>)? = null,
) : ReceiverInvitationRepository {
    val lookupTokens = CopyOnWriteArrayList<String>()
    val acceptTokens = CopyOnWriteArrayList<String>()

    @Volatile
    var createCalls: Int = 0
        private set

    override suspend fun create(): Result<ReceiverInvitationCreated> {
        createCalls += 1
        onCreate?.let { return it() }
        return Result.success(created)
    }

    override suspend fun lookup(token: String): Result<ReceiverInvitationLookup> {
        lookupTokens += token
        onLookup?.let { return it(token) }
        return Result.success(lookup)
    }

    override suspend fun accept(token: String): Result<ReceiverInvitationAccepted> {
        acceptTokens += token
        onAccept?.let { return it(token) }
        return Result.success(accepted)
    }

    companion object {
        const val DEFAULT_TOKEN = "fake-invitation-token"
        const val DEFAULT_INVITER = "김혜성"
    }
}
