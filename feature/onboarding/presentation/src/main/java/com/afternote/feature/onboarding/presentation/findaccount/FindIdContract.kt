package com.afternote.feature.onboarding.presentation.findaccount

import com.afternote.core.model.FoundAccount
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

/** 아이디 찾기 화면이 [FindIdViewModel] 에 보내는 것 — 사용자가 하려는 것. */
sealed interface FindIdIntent : MviIntent {
    data class UpdateEmail(
        val value: String,
    ) : FindIdIntent

    data class UpdateCertificateCode(
        val value: String,
    ) : FindIdIntent

    data object RequestVerificationCode : FindIdIntent

    data object VerifyCode : FindIdIntent

    /** 스낵바로 표시한 [FindIdUiState.errorMessage] 를 되돌린다. */
    data object ConsumeError : FindIdIntent
}

/** 상태가 겪은 것. [FindIdViewModel] 만 만든다. */
sealed interface FindIdReducerEvent : ReducerEvent {
    data class EmailChanged(
        val value: String,
    ) : FindIdReducerEvent

    data class CertificateCodeChanged(
        val value: String,
    ) : FindIdReducerEvent

    data object CodeSendStarted : FindIdReducerEvent

    data object CodeSent : FindIdReducerEvent

    data class CodeSendFailed(
        val message: UiText,
    ) : FindIdReducerEvent

    data object CodeSendFinished : FindIdReducerEvent

    /** 쿨다운을 30초로 재장전한다. */
    data object CooldownReloaded : FindIdReducerEvent

    data object CooldownTicked : FindIdReducerEvent

    data object VerifyStarted : FindIdReducerEvent

    data class AccountFound(
        val account: FoundAccount,
    ) : FindIdReducerEvent

    /** 인증번호 무효 — 인라인 문구로 알린다. */
    data object VerificationRejected : FindIdReducerEvent

    data class VerifyFailed(
        val message: UiText,
    ) : FindIdReducerEvent

    data object VerifyFinished : FindIdReducerEvent

    data object ErrorConsumed : FindIdReducerEvent
}
