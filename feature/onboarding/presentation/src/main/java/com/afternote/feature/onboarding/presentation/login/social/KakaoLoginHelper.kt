package com.afternote.feature.onboarding.presentation.login.social

import android.app.Activity
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 카카오 SDK로부터 OAuth 액세스 토큰을 획득한다.
 *
 * 플랫폼 SDK가 [Activity] 컨텍스트를 요구하므로 이 로직은 UI 레이어에 위치한다.
 * Data 레이어(Repository)에는 획득한 액세스 토큰 문자열만 전달되어,
 * Data 레이어가 UI 컴포넌트(Activity)에 의존하지 않도록 한다.
 *
 * 디바이스에 카카오톡이 설치된 경우 앱 로그인을, 그 외에는 웹 계정 로그인을 수행하며,
 * 앱 로그인이 비정상 종료된 경우에는 웹 계정 로그인으로 폴백한다.
 * 콜백 기반 SDK API를 [suspendCancellableCoroutine]으로 감싸 코루틴 흐름으로 변환한다.
 */
suspend fun requestKakaoAccessToken(activity: Activity): Result<String> =
    suspendCancellableCoroutine { continuation ->
        // 공통 콜백: 카카오톡 앱 로그인·웹 계정 로그인 결과를 모두 처리한다.
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (continuation.isActive) {
                when {
                    error != null -> {
                        val failure =
                            if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                                UserCancelledAuthException()
                            } else {
                                error
                            }
                        continuation.resume(Result.failure(failure))
                    }
                    token != null -> continuation.resume(Result.success(token.accessToken))
                    else -> continuation.resume(Result.failure(IllegalStateException("카카오 로그인 실패: 결과값 없음")))
                }
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
            // 카카오톡 앱이 설치된 경우: 앱 로그인 시도
            UserApiClient.instance.loginWithKakaoTalk(activity) { token, error ->
                if (error != null) {
                    // 사용자가 의도적으로 취소한 경우에는 폴백 없이 즉시 실패 처리
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(UserCancelledAuthException()))
                        }
                        return@loginWithKakaoTalk
                    }
                    // 그 외 에러(네트워크·앱 오류 등)는 웹 계정 로그인으로 폴백
                    UserApiClient.instance.loginWithKakaoAccount(activity, callback = callback)
                } else {
                    callback(token, null)
                }
            }
        } else {
            // 카카오톡 앱이 없는 경우: 곧바로 웹 계정 로그인
            UserApiClient.instance.loginWithKakaoAccount(activity, callback = callback)
        }
    }
