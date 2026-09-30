package com.afternote.feature.setting.presentation.password

import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState

/**
 * 설정 > 비밀번호 변경 화면 상태.
 *
 * 입력 칸이 둘인 것은 서버 계약 그대로다 — BE `PasswordChangeRequest` 는 `currentPassword`·
 * `newPassword` 두 필드뿐이다. 「새 비밀번호 확인」 칸은 비밀번호 **찾기**(`auth/password/find`)의
 * `confirmPassword` 계약에만 있고 변경에는 없어 두지 않는다.
 */
internal data class PasswordChangeUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val isSubmitting: Boolean = false,
    /**
     * 입력 아래에 그대로 남는 실패 안내. 다음 입력·다음 제출이 지운다.
     *
     * 일회성 신호가 아니다 — 어느 칸을 고쳐야 하는지 읽는 동안 남아 있어야 한다.
     */
    val errorMessage: UiText? = null,
    /** 변경 완료 신호. 화면이 한 번 소비하고 [PasswordChangeIntent.ConsumeChanged] 로 되돌린다(#228). */
    val changed: PasswordChanged? = null,
) : UiState {
    val isNewPasswordRuleSatisfied: Boolean get() = SettingPasswordRule.isSatisfied(newPassword)

    /**
     * 제출 가능 여부.
     *
     * 현재 비밀번호는 비어 있지만 않으면 된다 — 규칙 검사를 걸면 옛 규칙으로 만든 비밀번호를 쓰는
     * 계정이 변경 자체를 못 한다([SettingPasswordRule]). 새 비밀번호가 현재와 같은지는 서버만
     * 판정할 수 있다(BE `NEWPASSWORD_MATCH`) — 클라는 현재 비밀번호의 평문을 비교 근거로 삼지 않는다.
     */
    val isSubmitEnabled: Boolean
        get() = !isSubmitting && currentPassword.isNotEmpty() && isNewPasswordRuleSatisfied
}

/**
 * [PasswordChangeUiState.changed] 가 나르는 완료 신호.
 *
 * `Boolean` 대신 전용 타입인 것은 소비 관용구 `ObserveSignal` 이 non-null 값을 키로 쓰기 때문이다 —
 * `false` 도 값이라 플래그로 두면 화면 진입 즉시 한 번 소비된다.
 */
internal data object PasswordChanged

internal sealed interface PasswordChangeIntent : MviIntent {
    data class UpdateCurrentPassword(
        val value: String,
    ) : PasswordChangeIntent

    data class UpdateNewPassword(
        val value: String,
    ) : PasswordChangeIntent

    data object Submit : PasswordChangeIntent

    data object ConsumeChanged : PasswordChangeIntent
}

internal sealed interface PasswordChangeReducerEvent : ReducerEvent {
    data class CurrentPasswordChanged(
        val value: String,
    ) : PasswordChangeReducerEvent

    data class NewPasswordChanged(
        val value: String,
    ) : PasswordChangeReducerEvent

    data object SubmitStarted : PasswordChangeReducerEvent

    data object Succeeded : PasswordChangeReducerEvent

    data class Failed(
        val message: UiText,
    ) : PasswordChangeReducerEvent

    data object ChangedConsumed : PasswordChangeReducerEvent
}
