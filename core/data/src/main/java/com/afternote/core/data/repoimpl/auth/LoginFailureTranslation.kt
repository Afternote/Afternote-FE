package com.afternote.core.data.repoimpl.auth

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.network.model.ApiException
import java.io.IOException

// [ApiException.code] 는 HTTP 상태가 아니라 응답 본문의 비즈니스 코드다 — 대역 판정(4xx 통과)으로
// 바꾸지 말 것. 5xx 에 붙는 코드도 있어 대역으로는 사용자 거절과 장애가 갈리지 않는다.
private const val CODE_USER_NOT_FOUND = 1201
private const val CODE_PASSWORD_MISMATCH = 1202
private const val CODE_SOCIAL_LOGIN_FAILED = 1208
private const val CODE_UNSUPPORTED_SOCIAL_LOGIN = 1209
private const val CODE_SOCIAL_SIGNUP_ACCOUNT = 1702

/**
 * 로그인 실패를 도메인 예외로 옮긴다 — 가르는 신호는 서버 봉투의 `code` 뿐이고 `message` 는
 * 옮기지 않는다(BE#92 — 사용자 노출용이라는 규정이 없어 계약이 아니다). 사유가 확인된 실패만
 * 치환하고 나머지는 그대로 두어, 소비처가 일반 문구로 내려앉는다(5xx 본문 실측 #511).
 *
 * 서버 응답 실패인 [ApiException]과 전송 실패인 [IOException]은 서로 다른 타입 계열이다. 취소는
 * 다시 보지 않는다 — 호출부가 전부 [runCatchingCancellable]이라 `CancellationException`이 [Result]에
 * 담긴 채로 도달하지 않는다.
 */
internal fun <T> Result<T>.mapLoginFailure(): Result<T> =
    when (val exception = exceptionOrNull()) {
        is ApiException -> {
            when (exception.code) {
                CODE_USER_NOT_FOUND, CODE_PASSWORD_MISMATCH -> {
                    Result.failure(CoreAuthFailure.InvalidLoginCredentials(exception))
                }

                CODE_SOCIAL_LOGIN_FAILED, CODE_UNSUPPORTED_SOCIAL_LOGIN -> {
                    Result.failure(CoreAuthFailure.SocialLoginRejected(exception))
                }

                // 소셜로 가입해 로컬 비밀번호가 없는 계정에 이메일 로그인을 시도한 것(BE
                // `AuthService.login` 이 `password == null` 을 이 코드로 거절한다). 자격 거절과 가르는
                // 이유는 안내가 갈리기 때문이다 — 입력을 고쳐서 될 일이 아니라 로그인 수단이 틀렸다.
                CODE_SOCIAL_SIGNUP_ACCOUNT -> {
                    Result.failure(CoreAuthFailure.SocialSignUpAccount(exception))
                }

                else -> {
                    this
                }
            }
        }

        is IOException -> {
            Result.failure(CoreAuthFailure.NetworkUnavailable(exception))
        }

        else -> {
            this
        }
    }
