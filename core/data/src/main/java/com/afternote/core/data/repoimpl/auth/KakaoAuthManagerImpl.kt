package com.afternote.core.data.repoimpl.auth

import android.app.Activity
import com.afternote.core.domain.repository.auth.KakaoAuthManager
import com.afternote.core.domain.repository.auth.UserCancelledAuthException
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class KakaoAuthManagerImpl
    @Inject
    constructor() : KakaoAuthManager {
        override suspend fun login(activity: Activity): Result<String> =
            suspendCancellableCoroutine { continuation ->
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
                            else -> continuation.resume(Result.failure(IllegalStateException("카카오 로그인 실패")))
                        }
                    }
                }

                if (UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
                    UserApiClient.instance.loginWithKakaoTalk(activity) { token, error ->
                        if (error != null) {
                            if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                                if (continuation.isActive) {
                                    continuation.resume(Result.failure(UserCancelledAuthException()))
                                }
                                return@loginWithKakaoTalk
                            }
                            UserApiClient.instance.loginWithKakaoAccount(activity, callback = callback)
                        } else {
                            callback(token, null)
                        }
                    }
                } else {
                    UserApiClient.instance.loginWithKakaoAccount(activity, callback = callback)
                }
            }
    }
