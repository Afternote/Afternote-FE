package com.afternote.core.domain.push

/**
 * 이 기기의 현재 FCM 등록 토큰을 돌려준다 (#1493).
 *
 * 토큰 발급은 Firebase SDK 몫이고 그 의존은 `app` 에만 있다. 코어가 «토큰을 어떻게 얻는가» 를
 * 모른 채 «누구에게 등록하는가» 만 다루도록 이 경계를 둔다.
 *
 * 두 경로를 나눠 두는 이유는 **등록 시퀀스를 강제하는가** 하나다. 아래 각 함수 설명 참고.
 */
interface DevicePushTokenProvider {
    /**
     * 등록 시퀀스를 강제한 뒤 식별자를 읽는다 — 서버에 등록하러 가는 경로용이다.
     *
     * 발급에 실패했거나 Google Play 서비스가 없는 기기면 null. 구현은 예외를 올리지 않는다.
     */
    suspend fun currentToken(): String?

    /**
     * 이미 발급돼 있는 식별자만 읽는다. 등록 시퀀스를 강제하지 않는다 — 해제(로그아웃) 경로용이다.
     *
     * 해제에 [currentToken] 을 쓰면 서버에서 지우기 직전에 기기를 FCM 에 다시 등록하게 되고,
     * 그 결과로 뜬 회전 통보가 아직 살아 있는 세션을 타고 재등록(`PUT`)으로 돌아와 해제(`DELETE`)
     * 와 경합한다. 없는 값을 만들지 않는 이 경로에는 그 왕복이 없다.
     *
     * 발급된 적이 없거나 조회에 실패하면 null. 구현은 예외를 올리지 않는다.
     */
    suspend fun existingToken(): String?
}
