package com.afternote.core.domain.testing

import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.model.user.Passkey
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserPushSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * [UserRepository] fake 정본 (#1030, #1041).
 *
 * 기본은 사용자·수신자·설정을 메모리에 저장한다. 호출은 모두 기록하고, 경합 게이트나
 * 실패 응답처럼 저장소 상태만으로 표현할 수 없는 시나리오는 `onX` 로 갈아끼운다.
 */
class FakeUserRepository(
    @Volatile var profile: User = DEFAULT_USER,
    receivers: List<Receiver> = listOf(DEFAULT_RECEIVER),
    @Volatile var pushSetting: UserPushSetting = DEFAULT_PUSH_SETTING,
    @Volatile var connectedAccounts: UserConnectedAccount = defaultConnectedAccounts(profile.email),
    @Volatile var passkeys: List<Passkey> = emptyList(),
    receiverDetails: Map<Long, ReceiverDetail> = emptyMap(),
    deliveryConditions: Map<Long, ReceiverDeliveryConditions> = emptyMap(),
    var onReceiverListFlow: (() -> Flow<List<Receiver>>)? = null,
    var onGetReceivers: (suspend () -> List<Receiver>)? = null,
    var onCreateReceiver: (suspend (String, String, String?, String?, String?) -> ReceiverCreated)? = null,
    var onGetReceiverDetail: (suspend (Long) -> ReceiverDetail)? = null,
    var onUpdateReceiver: (suspend (Long, String, String, String, String) -> Receiver)? = null,
    var onUpdateReceiverMessage: (suspend (Long, String) -> Unit)? = null,
    var onGetMyProfile: (suspend () -> User)? = null,
    var onUpdateMyProfile: (suspend (String?, String?, String?) -> User)? = null,
    var onDeleteAccount: (suspend () -> Unit)? = null,
    var onGetPasskeys: (suspend () -> List<Passkey>)? = null,
    var onGetPasskeyRegisterOptions: (suspend () -> String)? = null,
    var onRegisterPasskey: (suspend (String) -> Passkey)? = null,
    var onLogActivity: (suspend () -> Unit)? = null,
    var onGetMyPushSettings: (suspend () -> UserPushSetting)? = null,
    var onUpdateMyPushSettings: (suspend (Boolean?, Boolean?, Boolean?) -> UserPushSetting)? = null,
    var onGetConnectedAccounts: (suspend () -> UserConnectedAccount)? = null,
    var onLinkConnectedAccount: (suspend (String, String) -> UserConnectedAccount)? = null,
    var onUnlinkConnectedAccount: (suspend (String) -> UserConnectedAccount)? = null,
    var onGetReceiverDeliveryConditions: (suspend (Long) -> ReceiverDeliveryConditions)? = null,
    var onUpdateReceiverDeliveryConditions: (suspend (Long, List<DeliveryConditionItem>) -> ReceiverDeliveryConditions)? = null,
) : UserRepository {
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
    private val getProfileCounter = AtomicInteger()
    private val deleteAccountCounter = AtomicInteger()
    private val getPasskeysCounter = AtomicInteger()
    private val logActivityCounter = AtomicInteger()
    private val getPushSettingsCounter = AtomicInteger()
    private val getConnectedAccountsCounter = AtomicInteger()

    val receiverCreateCalls = CopyOnWriteArrayList<ReceiverCreateCall>()
    val receiverDetailCalls = CopyOnWriteArrayList<Long>()
    val receiverUpdateCalls = CopyOnWriteArrayList<ReceiverUpdateCall>()
    val receiverMessageCalls = CopyOnWriteArrayList<ReceiverMessageCall>()
    val profileUpdateCalls = CopyOnWriteArrayList<ProfileUpdateCall>()
    val pushUpdateCalls = CopyOnWriteArrayList<PushUpdateCall>()
    val connectedLinkCalls = CopyOnWriteArrayList<ConnectedAccountLinkCall>()
    val connectedUnlinkCalls = CopyOnWriteArrayList<String>()
    val deliveryLoadCalls = CopyOnWriteArrayList<Long>()
    val deliveryUpdateCalls = CopyOnWriteArrayList<DeliveryUpdateCall>()

    val receiverListFlowCalls: Int get() = receiverListFlowCounter.get()
    val getReceiversCalls: Int get() = getReceiversCounter.get()
    val receiverCalls: Int get() = getReceiversCounter.get()
    val getProfileCalls: Int get() = getProfileCounter.get()
    val profileCalls: Int get() = getProfileCounter.get()
    val deleteAccountCalls: Int get() = deleteAccountCounter.get()
    val getPasskeysCalls: Int get() = getPasskeysCounter.get()
    val logActivityCalls: Int get() = logActivityCounter.get()
    val getMyPushSettingsCalls: Int get() = getPushSettingsCounter.get()
    val getConnectedAccountsCalls: Int get() = getConnectedAccountsCounter.get()
    val pushSettingUpdates: List<Triple<Boolean?, Boolean?, Boolean?>>
        get() = pushUpdateCalls.map { Triple(it.timeLetter, it.mindRecord, it.afterNote) }

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
        email: String?,
        message: String?,
    ): ReceiverCreated {
        receiverCreateCalls += ReceiverCreateCall(name, relation, phone, email, message)
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
        receiverUpdateCalls += ReceiverUpdateCall(receiverId, name, phone, relation, email)
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
        receiverMessageCalls += ReceiverMessageCall(receiverId, message)
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

    override suspend fun getMyProfile(): User {
        getProfileCounter.incrementAndGet()
        onGetMyProfile?.let { return it() }
        return profile
    }

    override suspend fun updateMyProfile(
        name: String?,
        phone: String?,
        profileImageUrl: String?,
    ): User {
        profileUpdateCalls += ProfileUpdateCall(name, phone, profileImageUrl)
        onUpdateMyProfile?.let { return it(name, phone, profileImageUrl) }
        profile =
            profile.copy(
                name = name ?: profile.name,
                phone = phone ?: profile.phone,
                profileImageUrl = profileImageUrl ?: profile.profileImageUrl,
            )
        return profile
    }

    override suspend fun deleteAccount() {
        deleteAccountCounter.incrementAndGet()
        onDeleteAccount?.invoke()
    }

    override suspend fun getPasskeys(): List<Passkey> {
        getPasskeysCounter.incrementAndGet()
        onGetPasskeys?.let { return it() }
        return passkeys
    }

    override suspend fun getPasskeyRegisterOptions(): String {
        onGetPasskeyRegisterOptions?.let { return it() }
        return "{}"
    }

    override suspend fun registerPasskey(credentialJson: String): Passkey {
        onRegisterPasskey?.let { return it(credentialJson) }
        val id = (passkeys.maxOfOrNull(Passkey::id) ?: 0L) + 1L
        val created = Passkey(id = id, displayName = "패스키", createdAt = "2026-01-01T00:00:00")
        passkeys = passkeys + created
        return created
    }

    override suspend fun logActivity() {
        logActivityCounter.incrementAndGet()
        onLogActivity?.invoke()
    }

    override suspend fun getMyPushSettings(): UserPushSetting {
        getPushSettingsCounter.incrementAndGet()
        onGetMyPushSettings?.let { return it() }
        return pushSetting
    }

    override suspend fun updateMyPushSettings(
        timeLetter: Boolean?,
        mindRecord: Boolean?,
        afterNote: Boolean?,
    ): UserPushSetting {
        pushUpdateCalls += PushUpdateCall(timeLetter, mindRecord, afterNote)
        onUpdateMyPushSettings?.let { return it(timeLetter, mindRecord, afterNote) }
        pushSetting =
            UserPushSetting(
                timeLetter = timeLetter ?: pushSetting.timeLetter,
                mindRecord = mindRecord ?: pushSetting.mindRecord,
                afterNote = afterNote ?: pushSetting.afterNote,
            )
        return pushSetting
    }

    override suspend fun getConnectedAccounts(): UserConnectedAccount {
        getConnectedAccountsCounter.incrementAndGet()
        onGetConnectedAccounts?.let { return it() }
        return connectedAccounts
    }

    override suspend fun linkConnectedAccount(
        provider: String,
        accessToken: String,
    ): UserConnectedAccount {
        connectedLinkCalls += ConnectedAccountLinkCall(provider, accessToken)
        onLinkConnectedAccount?.let { return it(provider, accessToken) }
        connectedAccounts = connectedAccounts.withProvider(provider, connected = true)
        return connectedAccounts
    }

    override suspend fun unlinkConnectedAccount(provider: String): UserConnectedAccount {
        connectedUnlinkCalls += provider
        onUnlinkConnectedAccount?.let { return it(provider) }
        connectedAccounts = connectedAccounts.withProvider(provider, connected = false)
        return connectedAccounts
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
        deliveryUpdateCalls += DeliveryUpdateCall(receiverId, conditions.toList())
        onUpdateReceiverDeliveryConditions?.let { return it(receiverId, conditions) }
        val stored = ReceiverDeliveryConditions(receiverId, conditions.toList())
        deliveryConditions[receiverId] = stored
        return stored.copy(conditions = stored.conditions.toList())
    }

    data class ProfileUpdateCall(
        val name: String?,
        val phone: String?,
        val profileImageUrl: String?,
    )

    data class PushUpdateCall(
        val timeLetter: Boolean?,
        val mindRecord: Boolean?,
        val afterNote: Boolean?,
    )

    data class ReceiverCreateCall(
        val name: String,
        val relation: String,
        val phone: String?,
        val email: String?,
        val message: String?,
    )

    data class ReceiverUpdateCall(
        val receiverId: Long,
        val name: String,
        val phone: String,
        val relation: String,
        val email: String,
    )

    data class ReceiverMessageCall(
        val receiverId: Long,
        val message: String,
    )

    data class ConnectedAccountLinkCall(
        val provider: String,
        val accessToken: String,
    )

    data class DeliveryUpdateCall(
        val receiverId: Long,
        val conditions: List<DeliveryConditionItem>,
    )

    companion object {
        private val DEFAULT_USER = User("테스트 사용자", "test@afternote.local", null, null)
        private val DEFAULT_RECEIVER = Receiver(7L, "김수신", "가족", "fake-auth-7")
        private val DEFAULT_PUSH_SETTING = UserPushSetting(true, true, true)

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
                onDeleteAccount = { unexpectedCall("UserRepository.deleteAccount") },
                onGetPasskeys = { unexpectedCall("UserRepository.getPasskeys") },
                onGetPasskeyRegisterOptions = { unexpectedCall("UserRepository.getPasskeyRegisterOptions") },
                onRegisterPasskey = { unexpectedCall("UserRepository.registerPasskey") },
                onLogActivity = { unexpectedCall("UserRepository.logActivity") },
                onGetMyPushSettings = { unexpectedCall("UserRepository.getMyPushSettings") },
                onUpdateMyPushSettings = { _, _, _ -> unexpectedCall("UserRepository.updateMyPushSettings") },
                onGetConnectedAccounts = { unexpectedCall("UserRepository.getConnectedAccounts") },
                onLinkConnectedAccount = { _, _ -> unexpectedCall("UserRepository.linkConnectedAccount") },
                onUnlinkConnectedAccount = { unexpectedCall("UserRepository.unlinkConnectedAccount") },
                onGetReceiverDeliveryConditions = { unexpectedCall("UserRepository.getReceiverDeliveryConditions") },
                onUpdateReceiverDeliveryConditions = { _, _ -> unexpectedCall("UserRepository.updateReceiverDeliveryConditions") },
            )

        private fun defaultConnectedAccounts(email: String): UserConnectedAccount =
            UserConnectedAccount(true, false, false, false, false, email, null, null, null, null)
    }
}

private fun UserConnectedAccount.withProvider(
    provider: String,
    connected: Boolean,
): UserConnectedAccount =
    when (provider.lowercase()) {
        "google" -> copy(google = connected, googleEmail = googleEmail.takeIf { connected })
        "naver" -> copy(naver = connected, naverEmail = naverEmail.takeIf { connected })
        "kakao" -> copy(kakao = connected, kakaoEmail = kakaoEmail.takeIf { connected })
        "apple" -> copy(apple = connected, appleEmail = appleEmail.takeIf { connected })
        "local" -> copy(local = connected, localEmail = localEmail.takeIf { connected })
        else -> error("지원하지 않는 연결 계정 provider: $provider")
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
