package com.afternote.afternote_fe.test

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserPushSetting
import com.afternote.feature.afternote.domain.AfternoteType

fun afternoteEditorSavedStateHandle(
    initialType: AfternoteType,
    itemId: Long? = null,
): SavedStateHandle =
    SavedStateHandle(
        buildMap {
            put("initialType", initialType)
            itemId?.let { put("itemId", it) }
        },
    )

/**
 * 기존 app androidTest 공용 Auth fake 의 기본 허용 범위를 정본 fixture 위에 보존한다.
 * 소셜 로그인과 토큰 회전은 각 테스트가 명시적으로 열지 않으면 호출 오류다.
 */
fun appTestAuthRepository(loggedIn: Boolean = false): FakeAuthRepository =
    FakeAuthRepository.strict(loggedIn = loggedIn).apply {
        onIsLoggedIn = null
        onSaveSession = null
        onUpdateTokens = null
        onClearSession = null
        onGetAccessToken = null
        onGetRefreshToken = null
        onDefaultLogin = null
        onLogout = null
    }

/**
 * 기존 app androidTest 공용 User fake 의 기본 허용 범위를 정본 fixture 위에 보존한다.
 * 수신자 상세·수정, 계정 연결, 전달조건은 전용 시나리오가 `onX` 로 명시해야 열린다.
 */
fun appTestUserRepository(
    profile: User = DEFAULT_TEST_USER,
    receivers: List<Receiver> = listOf(testReceiver()),
    pushSetting: UserPushSetting = DEFAULT_TEST_PUSH_SETTING,
): FakeUserRepository =
    FakeUserRepository.strict().apply {
        this.profile = profile
        receiverState.value = receivers.toList()
        this.pushSetting = pushSetting
        connectedAccounts = defaultConnectedAccounts(profile.email)

        onReceiverListFlow = null
        onGetReceivers = null
        onCreateReceiver = null
        onGetMyProfile = null
        onUpdateMyProfile = null
        onDeleteAccount = null
        onLogActivity = null
        onGetMyPushSettings = null
        onUpdateMyPushSettings = null
        onGetConnectedAccounts = { defaultConnectedAccounts(this.profile.email) }
    }

class FakeErrorReporter : ErrorReporter {
    val failures = mutableListOf<Pair<Throwable, Map<String, String>>>()

    /** 기록된 마음의 기록 stage 목록. 콘솔 필터 값이라 문자열까지 고정한다 (#964). */
    val mindRecordStages: List<String>
        get() = failures.mapNotNull { it.second["mind_record_stage"] }

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        failures += throwable to attributes
    }
}

fun testReceiver(
    id: Long = 7L,
    name: String = "김수신",
): Receiver = Receiver(receiverId = id, name = name, relation = "가족", authCode = "fake-auth-$id")

private val DEFAULT_TEST_USER = User("테스트 사용자", "test@afternote.local", null, null)
private val DEFAULT_TEST_PUSH_SETTING = UserPushSetting(true, true, true)

private fun defaultConnectedAccounts(email: String): UserConnectedAccount =
    UserConnectedAccount(true, false, false, false, false, email, null, null, null, null)
