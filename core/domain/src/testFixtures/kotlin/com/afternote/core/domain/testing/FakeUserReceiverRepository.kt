package com.afternote.core.domain.testing

import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * [UserReceiverRepository] fake 정본 (#1282, #1030).
 *
 * 수신자 목록·상세·전달조건만 메모리에 담는다 — 프로필·계정·푸시 상태는 갖지 않는다.
 * 호출은 모두 기록하고, 경합 게이트나 실패 응답처럼 저장소 상태만으로 표현할 수 없는 시나리오는 `onX` 로 갈아끼운다.
 *
 * 호출 기록 타입([FakeUserRepository.ReceiverCreateCall] 등)은 아직 [FakeUserRepository] 안에 남는다 —
 * `feature:setting` 테스트가 `FakeUserRepository.ReceiverCreateCall` 로 import 하고 있어 지금 옮기면
 * 남의 모듈이 깨진다. 소비자가 좁은 fake 로 이관될 때 함께 옮긴다.
 */
class FakeUserReceiverRepository(
    receivers: List<Receiver> = listOf(DEFAULT_RECEIVER),
    receiverDetails: Map<Long, ReceiverDetail> = emptyMap(),
    deliveryConditions: Map<Long, ReceiverDeliveryConditions> = emptyMap(),
    var onReceiverListFlow: (() -> Flow<List<Receiver>>)? = null,
    var onGetReceivers: (suspend () -> List<Receiver>)? = null,
    var onCreateReceiver: (suspend (String, String, String?, String, String?) -> ReceiverCreated)? = null,
    var onGetReceiverDetail: (suspend (Long) -> ReceiverDetail)? = null,
    var onUpdateReceiver: (suspend (Long, String, String, String, String) -> Receiver)? = null,
    var onUpdateReceiverMessage: (suspend (Long, String) -> Unit)? = null,
    var onGetReceiverDeliveryConditions: (suspend (Long) -> ReceiverDeliveryConditions)? = null,
    var onUpdateReceiverDeliveryConditions: (suspend (Long, List<DeliveryConditionItem>) -> ReceiverDeliveryConditions)? = null,
) : UserReceiverRepository {
    val receiverState = MutableStateFlow(receivers.toList())
    val receiverDetails = ConcurrentHashMap(receiverDetails)
    val deliveryConditions =
        ConcurrentHashMap(
            deliveryConditions.mapValues { (_, value) ->
                value.copy(conditions = value.conditions.toList())
            },
        )

    private val receiverListFlowCounter = AtomicInteger()
    private val getReceiversCounter = AtomicInteger()

    val receiverCreateCalls = CopyOnWriteArrayList<FakeUserRepository.ReceiverCreateCall>()
    val receiverDetailCalls = CopyOnWriteArrayList<Long>()
    val receiverUpdateCalls = CopyOnWriteArrayList<FakeUserRepository.ReceiverUpdateCall>()
    val receiverMessageCalls = CopyOnWriteArrayList<FakeUserRepository.ReceiverMessageCall>()
    val deliveryLoadCalls = CopyOnWriteArrayList<Long>()
    val deliveryUpdateCalls = CopyOnWriteArrayList<FakeUserRepository.DeliveryUpdateCall>()

    val receiverListFlowCalls: Int get() = receiverListFlowCounter.get()
    val getReceiversCalls: Int get() = getReceiversCounter.get()
    val receiverCalls: Int get() = getReceiversCounter.get()

    override val receiverListFlow: Flow<List<Receiver>>
        get() {
            receiverListFlowCounter.incrementAndGet()
            return onReceiverListFlow?.invoke() ?: receiverState
        }

    override suspend fun getReceivers(): List<Receiver> {
        getReceiversCounter.incrementAndGet()
        onGetReceivers?.let { return it() }
        return receiverState.value
    }

    override suspend fun createReceiver(
        name: String,
        relation: String,
        phone: String?,
        email: String,
        message: String?,
    ): ReceiverCreated {
        receiverCreateCalls += FakeUserRepository.ReceiverCreateCall(name, relation, phone, email, message)
        onCreateReceiver?.let { return it(name, relation, phone, email, message) }
        val id = (receiverState.value.maxOfOrNull(Receiver::receiverId) ?: 0L) + 1L
        val authCode = "fake-auth-$id"
        receiverState.value = receiverState.value + Receiver(id, name, relation, authCode)
        receiverDetails[id] =
            ReceiverDetail(id, name, relation, phone, email, 0, 0, 0, message, authCode)
        return ReceiverCreated(id, authCode)
    }

    override suspend fun getReceiverDetail(receiverId: Long): ReceiverDetail {
        receiverDetailCalls += receiverId
        onGetReceiverDetail?.let { return it(receiverId) }
        return receiverDetails.computeIfAbsent(receiverId) {
            requireNotNull(receiverState.value.firstOrNull { it.receiverId == receiverId }).toDefaultDetail()
        }
    }

    override suspend fun updateReceiver(
        receiverId: Long,
        name: String,
        phone: String,
        relation: String,
        email: String,
    ): Receiver {
        receiverUpdateCalls += FakeUserRepository.ReceiverUpdateCall(receiverId, name, phone, relation, email)
        onUpdateReceiver?.let { return it(receiverId, name, phone, relation, email) }
        val current = requireNotNull(receiverState.value.firstOrNull { it.receiverId == receiverId })
        val updated = current.copy(name = name, relation = relation)
        receiverState.value = receiverState.value.map { if (it.receiverId == receiverId) updated else it }
        receiverDetails.compute(receiverId) { _, detail ->
            (detail ?: current.toDefaultDetail()).copy(name = name, phone = phone, relation = relation, email = email)
        }
        return updated
    }

    override suspend fun updateReceiverMessage(
        receiverId: Long,
        message: String,
    ) {
        receiverMessageCalls += FakeUserRepository.ReceiverMessageCall(receiverId, message)
        onUpdateReceiverMessage?.let {
            it(receiverId, message)
            return
        }
        val currentDetail =
            receiverDetails[receiverId]
                ?: requireNotNull(receiverState.value.firstOrNull { it.receiverId == receiverId }).toDefaultDetail()
        receiverDetails.compute(receiverId) { _, detail ->
            (detail ?: currentDetail).copy(message = message)
        }
    }

    override suspend fun getReceiverDeliveryConditions(receiverId: Long): ReceiverDeliveryConditions {
        deliveryLoadCalls += receiverId
        onGetReceiverDeliveryConditions?.let { return it(receiverId) }
        val stored = deliveryConditions[receiverId] ?: ReceiverDeliveryConditions(receiverId, emptyList())
        return stored.copy(conditions = stored.conditions.toList())
    }

    override suspend fun updateReceiverDeliveryConditions(
        receiverId: Long,
        conditions: List<DeliveryConditionItem>,
    ): ReceiverDeliveryConditions {
        deliveryUpdateCalls += FakeUserRepository.DeliveryUpdateCall(receiverId, conditions.toList())
        onUpdateReceiverDeliveryConditions?.let { return it(receiverId, conditions) }
        val stored = ReceiverDeliveryConditions(receiverId, conditions.toList())
        deliveryConditions[receiverId] = stored
        return stored.copy(conditions = stored.conditions.toList())
    }

    companion object {
        internal val DEFAULT_RECEIVER = Receiver(7L, "김수신", "가족", "fake-auth-7")

        fun strict(): FakeUserReceiverRepository =
            FakeUserReceiverRepository(
                receivers = emptyList(),
                onReceiverListFlow = { unexpectedCall("UserReceiverRepository.receiverListFlow") },
                onGetReceivers = { unexpectedCall("UserReceiverRepository.getReceivers") },
                onCreateReceiver = { _, _, _, _, _ -> unexpectedCall("UserReceiverRepository.createReceiver") },
                onGetReceiverDetail = { unexpectedCall("UserReceiverRepository.getReceiverDetail") },
                onUpdateReceiver = { _, _, _, _, _ -> unexpectedCall("UserReceiverRepository.updateReceiver") },
                onUpdateReceiverMessage = { _, _ -> unexpectedCall("UserReceiverRepository.updateReceiverMessage") },
                onGetReceiverDeliveryConditions = {
                    unexpectedCall("UserReceiverRepository.getReceiverDeliveryConditions")
                },
                onUpdateReceiverDeliveryConditions = { _, _ ->
                    unexpectedCall("UserReceiverRepository.updateReceiverDeliveryConditions")
                },
            )
    }
}

private fun Receiver.toDefaultDetail(): ReceiverDetail =
    ReceiverDetail(
        receiverId = receiverId,
        name = name,
        relation = relation,
        phone = null,
        email = null,
        dailyQuestionCount = 0,
        timeLetterCount = 0,
        afterNoteCount = 0,
        message = null,
        authCode = authCode,
    )
