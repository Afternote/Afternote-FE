package com.afternote.core.domain.testing

/**
 * [FakeUserProfileCacheRepository] 의 옛 이름 (#1433).
 *
 * 계약 개명에 맞춘 별칭이며 `feature:home` 테스트 이관까지만 남는다.
 */
@Deprecated(
    message = "FakeUserProfileCacheRepository 로 옮겨 주세요.",
    replaceWith = ReplaceWith("FakeUserProfileCacheRepository"),
)
typealias FakeUserProfileRepository = FakeUserProfileCacheRepository
