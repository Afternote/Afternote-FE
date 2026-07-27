package com.afternote.feature.onboarding.presentation.findaccount

import android.util.Patterns
import com.afternote.core.model.FoundAccount

/**
 * 아이디 찾기 화면 상태.
 *
 * 시안에 인증번호 만료 타이머가 없어 회원가입(`SignUpUiState.verificationRemainingSeconds`)과 달리
 * 만료 카운트다운을 두지 않는다. 만료 판정은 서버가 하고, 만료된 코드는 [verificationError] 로 표시된다.
 *
 * @property resendCooldownSeconds "재전송" 버튼 잠금의 남은 초 — 0 보다 크면 "재전송 (Ns)" 표시·비활성.
 *   발송 성공마다 30초로 재잠금되는 클라이언트 측 연타 방지이며, **인증번호 유효시간과 무관**하다
 *   (만료 판정은 서버 몫, 쿨다운 종료 ≠ 코드 만료).
 * @property foundAccount "확인" 성공 시 서버가 돌려준 계정. non-null 이면 "다음" 이 열린다.
 * @property verificationError 인증번호 불일치 — 시안상 인증번호 필드 아래 인라인 문구로 표시(스낵바 아님).
 * @property errorMessage 인증번호 불일치 외의 실패(네트워크 등) — 스낵바로 표시.
 */
data class FindIdUiState(
    val email: String = "",
    val certificateCode: String = "",
    val isSendingCode: Boolean = false,
    val isVerificationSent: Boolean = false,
    val isVerifying: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val foundAccount: FoundAccount? = null,
    val verificationError: String? = null,
    val errorMessage: String? = null,
) {
    /**
     * 이메일 형식 검사. [Patterns.EMAIL_ADDRESS] 는 컴파일된 정규식(`Pattern`) 상수라
     * `matcher(입력)` 으로 그 문자열 전용 실행기를 만든 뒤 `matches()`(**전체 일치** — 부분 검색
     * `find()` 와 다름) 로 판정하는 Java regex 2단계 API 를 쓴다. 회원가입과 동일 방식.
     */
    val isEmailFormatValid: Boolean
        get() = email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    /** 인증번호 발송/재발송 가능 여부. 쿨다운 중이거나 이메일 형식이 틀리면 막는다. */
    val isSendCodeEnabled: Boolean
        get() = !isSendingCode && resendCooldownSeconds == 0 && isEmailFormatValid

    /** 인증번호 필드의 "확인" 활성 여부. 발송 이력이 있고 자릿수를 채워야 열린다. */
    val isVerifyEnabled: Boolean
        get() = !isVerifying && isVerificationSent && certificateCode.length >= MIN_VERIFICATION_CODE_LENGTH

    /** 하단 "다음" 활성 여부. "확인" 으로 계정을 받아야만 결과 화면으로 넘어갈 수 있다. */
    val isNextEnabled: Boolean
        get() = foundAccount != null

    companion object {
        const val MIN_VERIFICATION_CODE_LENGTH = 6
    }
}
