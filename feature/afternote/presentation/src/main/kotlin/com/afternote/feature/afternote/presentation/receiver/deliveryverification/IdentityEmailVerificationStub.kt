package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import com.afternote.feature.afternote.presentation.receiver.deliveryverification.IdentityEmailVerificationStub.Companion.STUB_CODE
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 본인 확인 이메일 인증(designs 3·4) 의 in-memory stub — 백엔드 `receiver-auth/email/send|verify` 미구현 (이슈 #215).
 *
 * 실제 백엔드가 붙기 전까지 UI 흐름만 확인할 수 있도록 임시로 다음을 흉내낸다:
 * - `sendCode(email)` : 항상 성공으로 가정. 발급된 코드는 [STUB_CODE]. 약 500ms 지연.
 * - `verifyCode(email, code)` : 입력 코드가 [STUB_CODE] 와 일치하면 성공.
 *
 * 발신자 등록 stub ([com.afternote.feature.afternote.presentation.receiver.recordsbox.SenderRegistry]) 와
 * 동일하게 백엔드 도입 시 본 클래스를 도메인/데이터 레이어 Repository 로 대체한다.
 */
@Singleton
class IdentityEmailVerificationStub
    @Inject
    constructor() {
        suspend fun sendCode(email: String): Result<Unit> {
            if (!isValidEmail(email)) {
                return Result.failure(IllegalArgumentException("invalid email"))
            }
            delay(SEND_DELAY_MS)
            return Result.success(Unit)
        }

        suspend fun verifyCode(
            email: String,
            code: String,
        ): Result<Unit> {
            if (!isValidEmail(email)) {
                return Result.failure(IllegalArgumentException("invalid email"))
            }
            delay(VERIFY_DELAY_MS)
            return if (code.trim() == STUB_CODE) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("code mismatch"))
            }
        }

        private companion object {
            const val STUB_CODE = "000000"
            const val SEND_DELAY_MS = 500L
            const val VERIFY_DELAY_MS = 300L
        }
    }

private fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
