package com.afternote.core.data.repoimpl.account

import com.afternote.core.domain.error.EmailAlreadyRegisteredException
import com.afternote.core.domain.error.EmailVerificationException
import com.afternote.core.domain.error.NetworkUnavailableException
import com.afternote.core.network.model.ApiException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** 인증번호 무효(불일치/만료/미존재 통합) — 아이디 찾기도 회원가입 인증과 같은 값을 쓴다. */
private const val CODE_INVALID_VERIFICATION = 1207

/** 이미 가입된 이메일 (`POST auth/email/send` 409, 2026-07-30 실기기 logcat). */
private const val CODE_EMAIL_ALREADY_REGISTERED = 1200

/**
 * 계정 API 실패를 도메인 예외로 옮긴다 — presentation 이 `core:network` 를 모른 채 타입만으로
 * 분기하게 하는 것이 목적이다(#646).
 *
 * 가르는 신호는 서버 봉투의 `code` 뿐이고 `message` 는 옮기지 않는다. 그 필드가 사용자 노출용이라는
 * 규정이 명세에 없어(BE#92 에서 확인 후 요청을 거둠) 계약으로 삼을 수 없고, 실제로 사용자용 문구와
 * 개발자 진단 문구가 섞여 있다. 표시 문구는 각 화면이 자기 리소스로 갖는다.
 */
internal fun <T> Result<T>.mapAccountFailure(): Result<T> {
    val exception = exceptionOrNull()
    if (exception is CancellationException) throw exception
    return when (exception) {
        // ApiException 이 IOException 을 상속하므로 순서가 곧 의미다 — 서버가 응답을 준 실패를
        // 전송 실패로 뭉뚱그리지 않도록 먼저 판정한다.
        is ApiException -> {
            when (exception.code) {
                CODE_INVALID_VERIFICATION -> {
                    Result.failure(EmailVerificationException(exception))
                }

                CODE_EMAIL_ALREADY_REGISTERED -> {
                    Result.failure(EmailAlreadyRegisteredException(exception))
                }

                else -> {
                    this
                }
            }
        }

        is IOException -> {
            Result.failure(NetworkUnavailableException(exception))
        }

        else -> {
            this
        }
    }
}
