package com.afternote.core.domain.repository

/**
 * [UserProfileCacheRepository] 의 옛 이름 (#1433).
 *
 * 이름이 서버 프로필 계약처럼 읽혀 [MyProfileRepository] 와 구분이 서지 않았다. 개명은 끝났고
 * 이 별칭은 `feature:home` 소비자 이관까지만 남는 과도기다 — 담당 모듈이 갈려 한 PR 로 못 옮긴다.
 */
@Deprecated(
    message = "실체가 로컬 캐시임을 드러내는 UserProfileCacheRepository 로 옮겨 주세요.",
    replaceWith = ReplaceWith("UserProfileCacheRepository"),
)
typealias UserProfileRepository = UserProfileCacheRepository
