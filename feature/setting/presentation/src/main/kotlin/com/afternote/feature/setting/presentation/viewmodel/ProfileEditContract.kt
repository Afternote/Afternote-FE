package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface ProfileEditIntent : MviIntent {
    data object RefreshOnReturn : ProfileEditIntent

    data object RetryLoad : ProfileEditIntent

    /**
     * 사진이 정해졌다 — 갤러리에서 골랐거나 즉석에서 찍었거나.
     *
     * 취소·촬영 실패는 이 Intent 를 내지 않는다. 「고르지 않았다」와 「지웠다」는 다른 일이고,
     * 이 이슈에 지우는 갈래는 없다.
     */
    data class SelectProfileImage(
        val uri: String,
    ) : ProfileEditIntent

    data class UpdateProfile(
        val name: String,
        val phone: String,
    ) : ProfileEditIntent

    data class ConsumeEvent(
        val event: ProfileEditEvent,
    ) : ProfileEditIntent
}

internal sealed interface ProfileEditReducerEvent : ReducerEvent {
    data object Loading : ProfileEditReducerEvent

    data class Loaded(
        val name: String,
        val phone: String,
        val email: String,
        val profileImageUrl: String?,
    ) : ProfileEditReducerEvent

    data object LoadFailed : ProfileEditReducerEvent

    data class ProfileImageSelected(
        val uri: String,
    ) : ProfileEditReducerEvent

    data object Updating : ProfileEditReducerEvent

    data class UpdateFinished(
        val event: ProfileEditEvent,
    ) : ProfileEditReducerEvent

    /**
     * 저장 코루틴이 어떤 식으로든 끝났다 — 진행 중 표시만 푼다.
     *
     * [UpdateFinished] 와 나뉘어 있는 이유는 취소다. 취소는 성공도 실패도 아니라 신호를 내지 않는데,
     * 그 갈래에서도 `isUpdating` 은 풀려야 한다. 성공·실패 갈래에서는 [UpdateFinished] 가 이미
     * 같은 값을 써 둔 뒤라 이 이벤트가 상태를 바꾸지 않는다(같은 값이면 방출도 없다).
     */
    data object UpdateStopped : ProfileEditReducerEvent

    data class EventConsumed(
        val event: ProfileEditEvent,
    ) : ProfileEditReducerEvent
}
