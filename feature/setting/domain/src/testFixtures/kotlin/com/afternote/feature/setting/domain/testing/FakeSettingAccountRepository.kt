package com.afternote.feature.setting.domain.testing

import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.feature.setting.domain.SettingAccountRepository
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * [SettingAccountRepository] fake 정본 (#1429).
 *
 * 연결 계정 상태만 메모리에 담는다 — 프로필·수신자·알림 상태는 갖지 않는다. 호출은 모두 기록하고,
 * 실패 응답이나 경합 게이트처럼 저장소 상태만으로 표현할 수 없는 시나리오는 `onX` 로 갈아끼운다.
 *
 * 호출 기록 타입 [ConnectedAccountLinkCall] 은 이 fake 가 소유한다 — 합본 `FakeUserRepository` 가
 * 좁은 사실을 들고 있으면 이관이 끝난 뒤에도 소비자가 합본을 계속 import 한다.
 */
class FakeSettingAccountRepository(
    @Volatile var connectedAccounts: UserConnectedAccount = DEFAULT_CONNECTED_ACCOUNTS,
    var onDeleteAccount: (suspend () -> Unit)? = null,
    var onGetConnectedAccounts: (suspend () -> UserConnectedAccount)? = null,
    var onLinkConnectedAccount: (suspend (String, String) -> UserConnectedAccount)? = null,
    var onUnlinkConnectedAccount: (suspend (String) -> UserConnectedAccount)? = null,
) : SettingAccountRepository {
    private val deleteAccountCounter = AtomicInteger()
    private val getConnectedAccountsCounter = AtomicInteger()

    val connectedLinkCalls = CopyOnWriteArrayList<ConnectedAccountLinkCall>()
    val connectedUnlinkCalls = CopyOnWriteArrayList<String>()

    val deleteAccountCalls: Int get() = deleteAccountCounter.get()
    val getConnectedAccountsCalls: Int get() = getConnectedAccountsCounter.get()

    override suspend fun deleteAccount() {
        deleteAccountCounter.incrementAndGet()
        onDeleteAccount?.invoke()
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

    data class ConnectedAccountLinkCall(
        val provider: String,
        val accessToken: String,
    )

    companion object {
        internal val DEFAULT_CONNECTED_ACCOUNTS =
            UserConnectedAccount(true, false, false, false, false, "test@afternote.local", null, null, null, null)

        fun strict(): FakeSettingAccountRepository =
            FakeSettingAccountRepository(
                onDeleteAccount = { unexpectedCall("SettingAccountRepository.deleteAccount") },
                onGetConnectedAccounts = { unexpectedCall("SettingAccountRepository.getConnectedAccounts") },
                onLinkConnectedAccount = { _, _ -> unexpectedCall("SettingAccountRepository.linkConnectedAccount") },
                onUnlinkConnectedAccount = { unexpectedCall("SettingAccountRepository.unlinkConnectedAccount") },
            )
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
