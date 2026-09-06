package com.afternote.core.domain.push

/**
 * 이 기기로 푸시를 보낼 때 서버가 쓰는 **대상 식별자**를 돌려준다 (#1493).
 *
 * 값의 정체는 앱 설정에 따라 갈린다 — 지금은 FID(Firebase Installation ID)이고, 매니페스트의
 * `firebase_messaging_installation_id_enabled` 를 끄면 FCM registration token 이 그 자리에 온다.
 * 그래서 이 계약은 값의 정체가 아니라 **역할**로 이름 붙인다 (#1570). 서버 계약
 * (`users/push-tokens`)이 이 값을 «token» 이라 부르지만 그 이름은 네트워크 경계까지고,
 * 코어까지 끌고 오면 「token 인데 실은 FID」라는 거짓말이 된다.
 *
 * 발급 수단은 Firebase SDK 몫이고 그 의존은 `app` 에만 있다. 코어가 «어떻게 얻는가» 를
 * 모른 채 «누구에게 등록하는가» 만 다루도록 이 경계를 둔다.
 *
 * 두 경로를 나눠 두는 이유는 **등록 시퀀스를 강제하는가** 하나다. 아래 각 함수 설명 참고.
 */
interface DevicePushTargetProvider {
    /**
     * 등록 시퀀스를 강제한 뒤 식별자를 읽는다 — 서버에 등록하러 가는 경로용이다.
     *
     * 발급에 실패했거나 Google Play 서비스가 없는 기기면 null. 구현은 예외를 올리지 않는다.
     */
    suspend fun currentTargetId(): String?

    /**
     * 이미 발급돼 있는 식별자만 읽는다. 등록 시퀀스를 강제하지 않는다 — 해제(로그아웃) 경로용이다.
     *
     * 해제에 [currentTargetId] 을 쓰면 서버에서 지우기 직전에 기기를 FCM 에 다시 등록하게 되고,
     * 그 결과로 뜬 회전 통보가 아직 살아 있는 세션을 타고 재등록(`PUT`)으로 돌아와 해제(`DELETE`)
     * 와 경합한다. 없는 값을 만들지 않는 이 경로에는 그 왕복이 없다.
     *
     * 발급된 적이 없거나 조회에 실패하면 null. 구현은 예외를 올리지 않는다.
     */
    suspend fun existingTargetId(): String?
}
