package com.afternote.feature.setting.domain.testing

import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserMarketingConsent
import com.afternote.core.model.user.UserPushSetting
import com.afternote.feature.setting.domain.SettingAccountRepository
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class FakeSettingAccountRepository(
    @Volatile var connectedAccounts: UserConnectedAccount =
        UserConnectedAccount(true, false, false, false, false, "test@afternote.local", null, null, null, null),
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
        fun strict(): FakeSettingAccountRepository =
            FakeSettingAccountRepository(
                onDeleteAccount = { error("SettingAccountRepository.deleteAccount") },
                onGetConnectedAccounts = { error("SettingAccountRepository.getConnectedAccounts") },
                onLinkConnectedAccount = { _, _ -> error("SettingAccountRepository.linkConnectedAccount") },
                onUnlinkConnectedAccount = { error("SettingAccountRepository.unlinkConnectedAccount") },
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
