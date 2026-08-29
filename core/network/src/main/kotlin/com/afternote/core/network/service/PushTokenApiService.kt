package com.afternote.core.network.service

import com.afternote.core.network.dto.DeletePushTokenRequestDto
import com.afternote.core.network.dto.PushTokenDto
import com.afternote.core.network.dto.RegisterPushTokenRequestDto
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.PUT

/**
 * FCM 기기 토큰 등록·해제 (#1493).
 *
 * 경로는 `users/push-tokens` 지만 [UserApiService] 와 따로 둔다 — 이 인터페이스를 구현하는 테스트 대역이
 * 네 곳(그중 하나는 setting 모듈)이라, 프로필·수신자 CRUD 와 수명이 다른 토큰 계약을 얹으면
 * 무관한 대역까지 전부 컴파일이 깨진다. 소비처는 `PushTokenRepositoryImpl` 하나다.
 */
interface PushTokenApiService {
    /**
     * FCM 기기 토큰 등록 — 로그인 확정·토큰 갱신 시 서버에 올린다 (#1493).
     *
     * 서버가 이 토큰을 알아야 푸시가 기기에 도달한다. 같은 토큰 재전송은 upsert 라 멱등이며,
     * 로그인 확정·`onNewToken` 마다 불러도 안전하다.
     */
    @PUT("users/push-tokens")
    suspend fun registerPushToken(
        @Body request: RegisterPushTokenRequestDto,
    ): BaseResponse<PushTokenDto>

    /**
     * FCM 기기 토큰 해제 — 로그아웃 시 이 기기로 더는 푸시가 가지 않게 한다 (#1493).
     *
     * 어떤 토큰을 지울지 본문으로 지정해야 해서 `@HTTP(hasBody = true)` 를 쓴다. 없는 토큰도 200 이다.
     * (회원 탈퇴는 서버가 `AccountWithdrawalService` 에서 전량 정리하므로 앱이 부르지 않는다.)
     */
    @HTTP(method = "DELETE", path = "users/push-tokens", hasBody = true)
    suspend fun deletePushToken(
        @Body request: DeletePushTokenRequestDto,
    ): BaseResponse<Unit>
}
