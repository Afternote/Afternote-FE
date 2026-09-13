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
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserMarketingConsent
import com.afternote.core.model.user.UserPushSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * [UserRepository] fake 정본 (#1030, #1041) — 전환기 합본 (#1282).
 *
 * 수신자·프로필 상태와 호출 기록은 책임별 fake 인 [FakeUserReceiverRepository]·[FakeMyProfileRepository]
 * 가 소유하고 여기서는 위임한다. 계정·푸시 6멤버만 이 fake 가 직접 갖는다 — core 에 좁은 계약을
 * 신설하지 않고 `feature:setting` 으로 곧장 내리기로 했기 때문이다 (#1429).
 *
 * 생성자·프로퍼티·호출 기록 타입·strict 문구는 전부 그대로다. 새 테스트는 이 합본이 아니라
 * 필요한 좁은 fake 만 쓰고, 기존 소비자가 이관되면 이 클래스는 사라진다.
 */
class FakeUserRepository private constructor(
    private val receiverFake: FakeUserReceiverRepository,
    private val myProfileFake: FakeMyProfileRepository,
    @Volatile var pushSetting: UserPushSetting,
    @Volatile var marketingConsent: UserMarketingConsent,
    @Volatile var connectedAccounts: UserConnectedAccount,
    var onDeleteAccount: (suspend () -> Unit)?,
    var onGetMyPushSettings: (suspend () -> UserPushSetting)?,
    var onUpdateMyPushSettings: (suspend (Boolean?, Boolean?, Boolean?) -> UserPushSetting)?,
    var onGetMyMarketingConsents: (suspend () -> UserMarketingConsent)?,
    var onUpdateMyMarketingConsents: (suspend (Boolean?, Boolean?, Boolean?) -> UserMarketingConsent)?,
    var onGetConnectedAccounts: (suspend () -> UserConnectedAccount)?,
    var onLinkConnectedAccount: (suspend (String, String) -> UserConnectedAccount)?,
    var onUnlinkConnectedAccount: (suspend (String) -> UserConnectedAccount)?,
) : UserRepository,
    UserReceiverRepository by receiverFake,
    MyProfileRepository by myProfileFake {
    @Suppress("LongParameterList")
    constructor(
        profile: User = FakeMyProfileRepository.DEFAULT_USER,
        receivers: List<Receiver> = listOf(FakeUserReceiverRepository.DEFAULT_RECEIVER),
        pushSetting: UserPushSetting = DEFAULT_PUSH_SETTING,
        marketingConsent: UserMarketingConsent = DEFAULT_MARKETING_CONSENT,
        connectedAccounts: UserConnectedAccount = defaultConnectedAccounts(profile.email),
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
        onDeleteAccount: (suspend () -> Unit)? = null,
        onGetMyPushSettings: (suspend () -> UserPushSetting)? = null,
        onUpdateMyPushSettings: (suspend (Boolean?, Boolean?, Boolean?) -> UserPushSetting)? = null,
        onGetMyMarketingConsents: (suspend () -> UserMarketingConsent)? = null,
        onUpdateMyMarketingConsents: (suspend (Boolean?, Boolean?, Boolean?) -> UserMarketingConsent)? = null,
        onGetConnectedAccounts: (suspend () -> UserConnectedAccount)? = null,
        onLinkConnectedAccount: (suspend (String, String) -> UserConnectedAccount)? = null,
        onUnlinkConnectedAccount: (suspend (String) -> UserConnectedAccount)? = null,
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
        pushSetting = pushSetting,
        marketingConsent = marketingConsent,
        connectedAccounts = connectedAccounts,
        onDeleteAccount = onDeleteAccount,
        onGetMyPushSettings = onGetMyPushSettings,
        onUpdateMyPushSettings = onUpdateMyPushSettings,
        onGetMyMarketingConsents = onGetMyMarketingConsents,
        onUpdateMyMarketingConsents = onUpdateMyMarketingConsents,
        onGetConnectedAccounts = onGetConnectedAccounts,
        onLinkConnectedAccount = onLinkConnectedAccount,
        onUnlinkConnectedAccount = onUnlinkConnectedAccount,
    )

    // 수신자 fake 위임 — 기존 소비자가 쓰던 이름·타입을 그대로 유지한다.
    val receiverState: MutableStateFlow<List<Receiver>> get() = receiverFake.receiverState
    val receiverDetails: ConcurrentHashMap<Long, ReceiverDetail> get() = receiverFake.receiverDetails
    val deliveryConditions: ConcurrentHashMap<Long, ReceiverDeliveryConditions> get() = receiverFake.deliveryConditions
    val receiverCreateCalls: CopyOnWriteArrayList<ReceiverCreateCall> get() = receiverFake.receiverCreateCalls
    val receiverDetailCalls: CopyOnWriteArrayList<Long> get() = receiverFake.receiverDetailCalls
    val receiverUpdateCalls: CopyOnWriteArrayList<ReceiverUpdateCall> get() = receiverFake.receiverUpdateCalls
    val receiverMessageCalls: CopyOnWriteArrayList<ReceiverMessageCall> get() = receiverFake.receiverMessageCalls
    val deliveryLoadCalls: CopyOnWriteArrayList<Long> get() = receiverFake.deliveryLoadCalls
    val deliveryUpdateCalls: CopyOnWriteArrayList<DeliveryUpdateCall> get() = receiverFake.deliveryUpdateCalls
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

    val profileUpdateCalls: CopyOnWriteArrayList<ProfileUpdateCall> get() = myProfileFake.profileUpdateCalls
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

    // 계정·푸시 설정 — #1429 로 `feature:setting` 에 내려갈 때까지 이 fake 가 직접 갖는다.
    private val deleteAccountCounter = AtomicInteger()
    private val getPushSettingsCounter = AtomicInteger()
    private val getMarketingConsentsCounter = AtomicInteger()
    private val getConnectedAccountsCounter = AtomicInteger()

    val pushUpdateCalls = CopyOnWriteArrayList<PushUpdateCall>()
    val marketingConsentUpdateCalls = CopyOnWriteArrayList<MarketingConsentUpdateCall>()
    val connectedLinkCalls = CopyOnWriteArrayList<ConnectedAccountLinkCall>()
    val connectedUnlinkCalls = CopyOnWriteArrayList<String>()

    val deleteAccountCalls: Int get() = deleteAccountCounter.get()
    val getMyPushSettingsCalls: Int get() = getPushSettingsCounter.get()
    val getMyMarketingConsentsCalls: Int get() = getMarketingConsentsCounter.get()
    val getConnectedAccountsCalls: Int get() = getConnectedAccountsCounter.get()
    val pushSettingUpdates: List<Triple<Boolean?, Boolean?, Boolean?>>
        get() = pushUpdateCalls.map { Triple(it.timeLetter, it.mindRecord, it.afterNote) }
    val marketingConsentUpdates: List<Triple<Boolean?, Boolean?, Boolean?>>
        get() = marketingConsentUpdateCalls.map { Triple(it.sms, it.email, it.push) }

    override suspend fun deleteAccount() {
        deleteAccountCounter.incrementAndGet()
        onDeleteAccount?.invoke()
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

    override suspend fun getMyMarketingConsents(): UserMarketingConsent {
        getMarketingConsentsCounter.incrementAndGet()
        onGetMyMarketingConsents?.let { return it() }
        return marketingConsent
    }

    override suspend fun updateMyMarketingConsents(
        sms: Boolean?,
        email: Boolean?,
        push: Boolean?,
    ): UserMarketingConsent {
        marketingConsentUpdateCalls += MarketingConsentUpdateCall(sms, email, push)
        onUpdateMyMarketingConsents?.let { return it(sms, email, push) }
        marketingConsent =
            UserMarketingConsent(
                sms = sms ?: marketingConsent.sms,
                email = email ?: marketingConsent.email,
                push = push ?: marketingConsent.push,
            )
        return marketingConsent
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

    data class MarketingConsentUpdateCall(
        val sms: Boolean?,
        val email: Boolean?,
        val push: Boolean?,
    )

    data class ReceiverCreateCall(
        val name: String,
        val relation: String,
        val phone: String?,
        val email: String,
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
        private val DEFAULT_PUSH_SETTING = UserPushSetting(true, true, true)
        private val DEFAULT_MARKETING_CONSENT = UserMarketingConsent(true, true, false)

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
                onGetMyPushSettings = { unexpectedCall("UserRepository.getMyPushSettings") },
                onUpdateMyPushSettings = { _, _, _ -> unexpectedCall("UserRepository.updateMyPushSettings") },
                onGetMyMarketingConsents = { unexpectedCall("UserRepository.getMyMarketingConsents") },
                onUpdateMyMarketingConsents = { _, _, _ -> unexpectedCall("UserRepository.updateMyMarketingConsents") },
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
