package com.afternote.feature.receiver.domain.testing

import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * [IdentityVerificationRepository] fake 정본.
 *
 * 기본 동작은 [verificationState]에 본인 확인 여부를 보관하며, 특수 시나리오만
 * [onIsVerified]·[onMarkVerified]로 갈아끼운다 (#1030, #1042).
 */
class FakeIdentityVerificationRepository(
    initialVerified: Boolean = false,
    var onIsVerified: (() -> Flow<Boolean>)? = null,
    var onMarkVerified: (suspend () -> Unit)? = null,
) : IdentityVerificationRepository {
    val verificationState = MutableStateFlow(initialVerified)

    private val isVerifiedAccessCounter = AtomicInteger()
    private val markVerifiedCallCounter = AtomicInteger()

    val isVerifiedAccessCount: Int
        get() = isVerifiedAccessCounter.get()

    val markVerifiedCallCount: Int
        get() = markVerifiedCallCounter.get()

    override val isVerified: Flow<Boolean>
        get() {
            isVerifiedAccessCounter.incrementAndGet()
            return onIsVerified?.invoke() ?: verificationState
        }

    override suspend fun markVerified() {
        markVerifiedCallCounter.incrementAndGet()
        onMarkVerified?.let {
            it()
            return
        }
        verificationState.value = true
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
