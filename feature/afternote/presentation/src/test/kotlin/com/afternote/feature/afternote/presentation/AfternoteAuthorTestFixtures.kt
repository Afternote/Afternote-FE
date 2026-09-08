package com.afternote.feature.afternote.presentation

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.domain.testing.FakeUserProfileCacheRepository
import com.afternote.core.domain.testing.FakeUserReceiverRepository
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.User
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute

/**
 * 이관 전 route 인자를 나르던 [SavedStateHandle] 에서 Nav3 진입 키를 만든다.
 *
 * 에디터 ViewModel 은 route 를 assisted 로 받고([AfternoteRoute.EditorFlowRoute]) 폼 스냅샷은 계속
 * [SavedStateHandle] 에 넣는다 — 두 벌로 쪼개면 테스트마다 같은 값을 두 번 적어야 해서, 기존
 * 핸들 픽스처에서 키를 파생한다.
 */
internal fun SavedStateHandle.editorFlowRoute(): AfternoteRoute.EditorFlowRoute =
    AfternoteRoute.EditorFlowRoute(
        itemId = get<Long>("itemId"),
        initialType = requireNotNull(get<AfternoteType>("initialType")) { "initialType 이 없는 에디터 핸들" },
    )

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

/** 에디터·수신자 선택이 사용하는 수신자 조회 계약만 허용한다. */
internal fun afternoteAuthorUserReceiverRepository(): FakeUserReceiverRepository =
    FakeUserReceiverRepository.strict().apply {
        receiverState.value = listOf(TEST_RECEIVER)
        onGetReceivers = null
    }

/** 작성자 상세가 사용하는 프로필 조회 계약만 허용한다. */
internal fun afternoteAuthorMyProfileRepository(): FakeMyProfileRepository =
    FakeMyProfileRepository.strict().apply {
        profile = TEST_USER
        onGetMyProfile = null
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
