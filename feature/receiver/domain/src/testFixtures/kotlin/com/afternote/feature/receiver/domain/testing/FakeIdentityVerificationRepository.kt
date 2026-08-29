package com.afternote.feature.receiver.domain.testing

import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * [IdentityVerificationRepository] fake 정본.
 *
 * 기본 동작은 [verifiedSenderIds]에 발신자별 본인 확인 여부를 보관하며(#597 발신자별 격리),
 * 특수 시나리오만 [onIsVerified]·[onMarkVerified]로 갈아끼운다 (#1030, #1042).
 */
class FakeIdentityVerificationRepository(
    initialVerifiedSenderIds: Set<String> = emptySet(),
    var onIsVerified: ((senderId: String) -> Flow<Boolean>)? = null,
    var onMarkVerified: (suspend (senderId: String) -> Unit)? = null,
) : IdentityVerificationRepository {
    val verifiedSenderIds = MutableStateFlow(initialVerifiedSenderIds)

    private val markVerifiedCallCounter = AtomicInteger()
    private val markVerifiedSenderIdsRecord = CopyOnWriteArrayList<String>()

    val markVerifiedCallCount: Int
        get() = markVerifiedCallCounter.get()

    /** [markVerified] 가 받은 senderId 호출 순서 기록 — "맞는 발신자에 기록했나" 단언용. */
    val markVerifiedSenderIds: List<String>
        get() = markVerifiedSenderIdsRecord.toList()

    override fun isVerified(senderId: String): Flow<Boolean> = onIsVerified?.invoke(senderId) ?: verifiedSenderIds.map { senderId in it }

    override suspend fun markVerified(senderId: String) {
        markVerifiedCallCounter.incrementAndGet()
        markVerifiedSenderIdsRecord.add(senderId)
        onMarkVerified?.let {
            it(senderId)
            return
        }
        verifiedSenderIds.update { it + senderId }
    }

    companion object {
        /** 모든 호출을 실패시키고, 시나리오가 실제로 쓰는 것만 `onX`로 연다. */
        fun strict(): FakeIdentityVerificationRepository =
            FakeIdentityVerificationRepository(
                onIsVerified = { unexpectedCall("IdentityVerificationRepository.isVerified") },
                onMarkVerified = { unexpectedCall("IdentityVerificationRepository.markVerified") },
            )
    }
}
