package com.afternote.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * 앱의 모든 Preferences DataStore 를 만들어 나눠주는 단일 창구 (#912).
 *
 * 각 모듈이 `preferencesDataStore(name = ...)` 델리게이트를 독립 선언하던 구조에서는
 * (1) 로그아웃 정리 대상 등록을 빼먹어도 어디서도 안 걸리고, (2) 파일명 충돌을 아무도 못 막았다.
 * 레지스트리 경유로 바꾸면 획득 시점에 수명([StoreScope])이 함께 등록되고, 같은 name 은
 * 항상 같은 인스턴스를 돌려받아(DataStore 는 파일당 인스턴스 1개가 강제 조건) 둘 다 구조적으로 풀린다.
 *
 * 반환 타입은 [DataStore] 그대로다 — `Flow` 관찰·원자적 다중 키 `edit` 등 DataStore API 를
 * 좁히지 않는다(#912 하지 말 것). 타입드 접근자(스키마)는 각 feature 의 DataSource 가 유지한다.
 */
interface LocalStoreRegistry {
    /**
     * `files/datastore/<name>.preferences_pb` 를 쓰는 DataStore 를 만들어 돌려주고,
     * [scope] 를 수명으로 등록한다. 같은 [name] 재요청은 같은 인스턴스를 돌려주며,
     * 같은 [name] 을 다른 [scope] 로 재요청하면 즉시 실패한다.
     *
     * [name] 은 저장 파일명 계약이다 — 바꾸면 기존 사용자의 데이터가 통째로 끊긴다 (#912 필수 주의).
     */
    fun store(
        name: String,
        scope: StoreScope,
    ): DataStore<Preferences>

    /**
     * [scope] 로 등록된 모든 저장소의 키를 비운다 (파일은 유지).
     *
     * 등록 이력은 디스크에도 남으므로, 이번 프로세스에서 아직 획득된 적 없는 저장소도
     * 함께 지워진다 — Hilt provider 는 최초 주입 때에야 실행되므로 in-memory 등록만으로는
     * "이전 프로세스에서 쓰고 이번 프로세스에서 안 연" 저장소가 새는 구멍이 있다.
     */
    suspend fun clearScope(scope: StoreScope)
}
