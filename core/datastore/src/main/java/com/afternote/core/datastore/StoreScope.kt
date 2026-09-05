package com.afternote.core.datastore

/**
 * 로컬 저장소(Preferences DataStore)의 수명 계약 (#912).
 *
 * 저장소는 [LocalStoreRegistry.store] 로 얻는 순간 scope 가 함께 등록되므로,
 * 세션 종료 시 정리 대상에서 빠뜨리는 것이 구조적으로 차단된다.
 */
enum class StoreScope {
    /** 로그인 세션에 귀속 — 로그아웃·회원 탈퇴 시 [LocalStoreRegistry.clearScope] 가 전부 비운다. */
    SESSION,

    /** 기기에 귀속 — 세션 종료와 무관하게 유지 (앱 삭제 전까지). */
    DEVICE,
}
