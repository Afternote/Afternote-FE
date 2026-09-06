package com.afternote.feature.receiver.domain.testing

import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderEntry
import com.afternote.feature.receiver.domain.repository.SenderRegistryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * [SenderRegistryRepository] fake 정본.
 *
 * 기본 경로는 [senderEntries]에 등록 순서대로 카드를 보관하는 성공 구현이다. 특수 시나리오만
 * `onX` 콜백으로 바꾼다. [strict]도 Flow 조회는 안전한 빈 목록으로 유지하며, 열지 않은 suspend
 * 호출은 예외를 던지지 않고 [Result.failure]로 돌려 androidTest 프로세스 크래시를 막는다.
 */
class FakeSenderRegistryRepository(
    initialSenders: List<SenderEntry> = emptyList(),
    var onRegister: (suspend (name: String) -> Result<SenderEntry>)? = null,
    var onFindById: (suspend (id: String) -> Result<SenderEntry?>)? = null,
    var onAttachIdentity: (suspend (id: String, masterKey: String, identity: ReceiverIdentity) -> Result<SenderEntry?>)? = null,
    var onUpdateVerificationStatus: (suspend (id: String, status: DeliveryVerificationStatus) -> Result<SenderEntry?>)? = null,
) : SenderRegistryRepository {
    val senderEntries = MutableStateFlow(initialSenders.toList())

    override val senders: Flow<List<SenderEntry>> = senderEntries.asStateFlow()

    override suspend fun register(name: String): Result<SenderEntry> {
        onRegister?.let { return it(name) }

        val entry = SenderEntry(id = UUID.randomUUID().toString(), name = name)
        senderEntries.update { it + entry }
        return Result.success(entry)
    }

    override suspend fun findById(id: String): Result<SenderEntry?> =
        onFindById?.invoke(id) ?: Result.success(senderEntries.value.firstOrNull { it.id == id })

    override suspend fun attachIdentity(
        id: String,
        masterKey: String,
        identity: ReceiverIdentity,
    ): Result<SenderEntry?> {
        onAttachIdentity?.let { return it(id, masterKey, identity) }
        return Result.success(
            updateById(id) { entry ->
                entry.copy(
                    masterKey = masterKey,
                    realSenderName = identity.senderName,
                    relation = identity.relation,
                )
            },
        )
    }

    override suspend fun updateVerificationStatus(
        id: String,
        status: DeliveryVerificationStatus,
    ): Result<SenderEntry?> {
        onUpdateVerificationStatus?.let { return it(id, status) }
        return Result.success(updateById(id) { it.copy(verificationStatus = status) })
    }

    private inline fun updateById(
        id: String,
        transform: (SenderEntry) -> SenderEntry,
    ): SenderEntry? {
        var updated: SenderEntry? = null
        senderEntries.update { entries ->
            entries.map { entry ->
                if (entry.id == id) {
                    transform(entry).also { updated = it }
                } else {
                    entry
                }
            }
        }
        return updated
    }

    companion object {
        /** 열지 않은 suspend 경로를 모두 실패 [Result]로 반환하는 crash-safe strict fake. */
        fun strict(initialSenders: List<SenderEntry> = emptyList()): FakeSenderRegistryRepository =
            FakeSenderRegistryRepository(
                initialSenders = initialSenders,
                onRegister = { Result.failure(unexpectedResultCall("SenderRegistryRepository.register")) },
                onFindById = { Result.failure(unexpectedResultCall("SenderRegistryRepository.findById")) },
                onAttachIdentity = { _, _, _ ->
                    Result.failure(unexpectedResultCall("SenderRegistryRepository.attachIdentity"))
                },
                onUpdateVerificationStatus = { _, _ ->
                    Result.failure(unexpectedResultCall("SenderRegistryRepository.updateVerificationStatus"))
                },
            )

        private fun unexpectedResultCall(method: String): IllegalStateException =
            IllegalStateException(
                "$method 는 이 시나리오에서 호출되면 안 됨",
            )
    }
}
