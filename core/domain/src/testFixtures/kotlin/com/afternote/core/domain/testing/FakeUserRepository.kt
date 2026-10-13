package com.afternote.core.domain.testing

import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.model.user.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [UserRepository] fake 정본 (#1030, #1041) — 전환기 합본 (#1282).
 *
 * 수신자·프로필 상태와 호출 기록은 책임별 fake 인 [FakeUserReceiverRepository]·[FakeMyProfileRepository]
 * 가 소유하고 여기서는 위임한다. 계정·푸시 8멤버는 `feature:setting:domain` 의 fake 2종으로 내려가
 * 이 합본에는 남지 않는다 (#1429).
 *
 * 생성자·프로퍼티·호출 기록 타입·strict 문구는 남은 범위 안에서 그대로다. 새 테스트는 이 합본이 아니라
 * 필요한 좁은 fake 만 쓰고, 기존 소비자가 이관되면 이 클래스는 사라진다.
 */
class FakeUserRepository private constructor(
    private val receiverFake: FakeUserReceiverRepository,
    private val myProfileFake: FakeMyProfileRepository,
) : UserRepository,
    UserReceiverRepository by receiverFake,
    MyProfileRepository by myProfileFake {
    @Suppress("LongParameterList")
    constructor(
        profile: User = FakeMyProfileRepository.DEFAULT_USER,
        receivers: List<Receiver> = listOf(FakeUserReceiverRepository.DEFAULT_RECEIVER),
        receiverDetails: Map<Long, ReceiverDetail> = emptyMap(),
        deliveryConditions: Map<Long, ReceiverDeliveryConditions> = emptyMap(),
        onReceiverListFlow: (() -> Flow<List<Receiver>>)? = null,
        onGetReceivers: (suspend () -> List<Receiver>)? = null,
        onCreateReceiver: (suspend (String, String, String?, String, String?) -> ReceiverCreated)? = null,
        onGetReceiverDetail: (suspend (Long) -> ReceiverDetail)? = null,
        onUpdateReceiver: (suspend (Long, String, String, String, String) -> Receiver)? = null,
        onUpdateReceiverMessage: (suspend (Long, String) -> Unit)? = null,
        onGetMyProfile: (suspend () -> User)? = null,
        onUpdateMyProfile: (suspend (String?, String?, String?) -> User)? = null,
        onGetReceiverDeliveryConditions: (suspend (Long) -> ReceiverDeliveryConditions)? = null,
        onUpdateReceiverDeliveryConditions: (
            suspend (Long, List<DeliveryConditionItem>) -> ReceiverDeliveryConditions
        )? = null,
    ) : this(
        receiverFake =
            FakeUserReceiverRepository(
                receivers = receivers,
                receiverDetails = receiverDetails,
                deliveryConditions = deliveryConditions,
                onReceiverListFlow = onReceiverListFlow,
                onGetReceivers = onGetReceivers,
                onCreateReceiver = onCreateReceiver,
                onGetReceiverDetail = onGetReceiverDetail,
                onUpdateReceiver = onUpdateReceiver,
                onUpdateReceiverMessage = onUpdateReceiverMessage,
                onGetReceiverDeliveryConditions = onGetReceiverDeliveryConditions,
                onUpdateReceiverDeliveryConditions = onUpdateReceiverDeliveryConditions,
            ),
        myProfileFake =
            FakeMyProfileRepository(
                profile = profile,
                onGetMyProfile = onGetMyProfile,
                onUpdateMyProfile = onUpdateMyProfile,
            ),
    )

    // 수신자 fake 위임 — 기존 소비자가 쓰던 이름·타입을 그대로 유지한다.
    val receiverState: MutableStateFlow<List<Receiver>> get() = receiverFake.receiverState
    val receiverDetails: ConcurrentHashMap<Long, ReceiverDetail> get() = receiverFake.receiverDetails
    val deliveryConditions: ConcurrentHashMap<Long, ReceiverDeliveryConditions> get() = receiverFake.deliveryConditions
    val receiverCreateCalls: CopyOnWriteArrayList<FakeUserReceiverRepository.ReceiverCreateCall>
        get() = receiverFake.receiverCreateCalls
    val receiverDetailCalls: CopyOnWriteArrayList<Long> get() = receiverFake.receiverDetailCalls
    val receiverUpdateCalls: CopyOnWriteArrayList<FakeUserReceiverRepository.ReceiverUpdateCall>
        get() = receiverFake.receiverUpdateCalls
    val receiverMessageCalls: CopyOnWriteArrayList<FakeUserReceiverRepository.ReceiverMessageCall>
        get() = receiverFake.receiverMessageCalls
    val deliveryLoadCalls: CopyOnWriteArrayList<Long> get() = receiverFake.deliveryLoadCalls
    val deliveryUpdateCalls: CopyOnWriteArrayList<FakeUserReceiverRepository.DeliveryUpdateCall>
        get() = receiverFake.deliveryUpdateCalls
    val receiverListFlowCalls: Int get() = receiverFake.receiverListFlowCalls
    val getReceiversCalls: Int get() = receiverFake.getReceiversCalls
    val receiverCalls: Int get() = receiverFake.receiverCalls

    var onReceiverListFlow: (() -> Flow<List<Receiver>>)?
        get() = receiverFake.onReceiverListFlow
        set(value) {
            receiverFake.onReceiverListFlow = value
        }

    var onGetReceivers: (suspend () -> List<Receiver>)?
        get() = receiverFake.onGetReceivers
        set(value) {
            receiverFake.onGetReceivers = value
        }

    var onCreateReceiver: (suspend (String, String, String?, String, String?) -> ReceiverCreated)?
        get() = receiverFake.onCreateReceiver
        set(value) {
            receiverFake.onCreateReceiver = value
        }

    var onGetReceiverDetail: (suspend (Long) -> ReceiverDetail)?
        get() = receiverFake.onGetReceiverDetail
        set(value) {
            receiverFake.onGetReceiverDetail = value
        }

    var onUpdateReceiver: (suspend (Long, String, String, String, String) -> Receiver)?
        get() = receiverFake.onUpdateReceiver
        set(value) {
            receiverFake.onUpdateReceiver = value
        }

    var onUpdateReceiverMessage: (suspend (Long, String) -> Unit)?
        get() = receiverFake.onUpdateReceiverMessage
        set(value) {
            receiverFake.onUpdateReceiverMessage = value
        }

    var onGetReceiverDeliveryConditions: (suspend (Long) -> ReceiverDeliveryConditions)?
        get() = receiverFake.onGetReceiverDeliveryConditions
        set(value) {
            receiverFake.onGetReceiverDeliveryConditions = value
        }

    var onUpdateReceiverDeliveryConditions: (suspend (Long, List<DeliveryConditionItem>) -> ReceiverDeliveryConditions)?
        get() = receiverFake.onUpdateReceiverDeliveryConditions
        set(value) {
            receiverFake.onUpdateReceiverDeliveryConditions = value
        }

    // 서버 정본 프로필 fake 위임.
    var profile: User
        get() = myProfileFake.profile
        set(value) {
            myProfileFake.profile = value
        }

    val profileUpdateCalls: CopyOnWriteArrayList<FakeMyProfileRepository.ProfileUpdateCall>
        get() = myProfileFake.profileUpdateCalls
    val getProfileCalls: Int get() = myProfileFake.getProfileCalls
    val profileCalls: Int get() = myProfileFake.profileCalls

    var onGetMyProfile: (suspend () -> User)?
        get() = myProfileFake.onGetMyProfile
        set(value) {
            myProfileFake.onGetMyProfile = value
        }

    var onUpdateMyProfile: (suspend (String?, String?, String?) -> User)?
        get() = myProfileFake.onUpdateMyProfile
        set(value) {
            myProfileFake.onUpdateMyProfile = value
        }

    companion object {
        fun strict(): FakeUserRepository =
            FakeUserRepository(
                receivers = emptyList(),
                onReceiverListFlow = { unexpectedCall("UserRepository.receiverListFlow") },
                onGetReceivers = { unexpectedCall("UserRepository.getReceivers") },
                onCreateReceiver = { _, _, _, _, _ -> unexpectedCall("UserRepository.createReceiver") },
                onGetReceiverDetail = { unexpectedCall("UserRepository.getReceiverDetail") },
                onUpdateReceiver = { _, _, _, _, _ -> unexpectedCall("UserRepository.updateReceiver") },
                onUpdateReceiverMessage = { _, _ -> unexpectedCall("UserRepository.updateReceiverMessage") },
                onGetMyProfile = { unexpectedCall("UserRepository.getMyProfile") },
                onUpdateMyProfile = { _, _, _ -> unexpectedCall("UserRepository.updateMyProfile") },
                onGetReceiverDeliveryConditions = { unexpectedCall("UserRepository.getReceiverDeliveryConditions") },
                onUpdateReceiverDeliveryConditions = { _, _ -> unexpectedCall("UserRepository.updateReceiverDeliveryConditions") },
            )
    }
}
