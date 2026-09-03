package com.afternote.feature.afternote.presentation

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeUserProfileCacheRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserPushSetting
import com.afternote.feature.afternote.domain.AfternoteType

internal fun afternoteEditorSavedStateHandle(
    initialType: AfternoteType,
    itemId: Long? = null,
): SavedStateHandle =
    SavedStateHandle(
        buildMap {
            put("initialType", initialType)
            itemId?.let { put("itemId", it) }
        },
    )

/** app androidTest 공용 helper가 열어 두던 UserRepository 경계만 그대로 허용한다. */
internal fun afternoteAuthorUserRepository(): FakeUserRepository =
    FakeUserRepository.strict().apply {
        profile = TEST_USER
        receiverState.value = listOf(TEST_RECEIVER)
        pushSetting = TEST_PUSH_SETTING
        connectedAccounts = testConnectedAccounts(profile.email)

        onReceiverListFlow = null
        onGetReceivers = null
        onCreateReceiver = null
        onGetMyProfile = null
        onUpdateMyProfile = null
        onDeleteAccount = null
        onGetMyPushSettings = null
        onUpdateMyPushSettings = null
        onGetConnectedAccounts = { testConnectedAccounts(profile.email) }
    }

/**
 * 작성자 흐름이 쓰는 [FakeUserProfileCacheRepository] — 이름 캐시 두 멤버만 열어 둔다.
 * 패스키 멤버까지 열면 상세·에디터가 건드리지 않는 계약이 조용히 통과한다.
 */
internal fun afternoteAuthorUserProfileRepository(cachedUserName: String? = null): FakeUserProfileCacheRepository =
    FakeUserProfileCacheRepository.strict().also {
        it.cachedUserName = cachedUserName
        it.onGetCachedUserName = null
        it.onSaveUserName = null
    }

internal object NoopAuthorErrorReporter : ErrorReporter {
    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) = Unit
}

private val TEST_USER = User("테스트 사용자", "test@afternote.local", null, null)
private val TEST_RECEIVER = Receiver(7L, "김수신", "가족", "fake-auth-7")
private val TEST_PUSH_SETTING = UserPushSetting(true, true, true)

private fun testConnectedAccounts(email: String): UserConnectedAccount =
    UserConnectedAccount(true, false, false, false, false, email, null, null, null, null)
