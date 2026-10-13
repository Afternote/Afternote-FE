package com.afternote.afternote_fe.update

/**
 * 지금 돌고 있는 설치본의 배포 정체성 (#1539).
 *
 * [ForceUpdateGate] 가 `BuildConfig` 를 직접 읽지 않고 이 값을 주입받는 이유는 단위 테스트다 —
 * `BuildConfig.VERSION_CODE` 는 빌드 시각에 고정돼 관문의 분기를 테스트로 덮을 수 없다.
 *
 * @property versionCode 서버에 보낼 이 설치본의 versionCode.
 * @property storeDistributed 이 빌드가 **스토어로 업데이트될 수 있는** 산출물인가.
 *   Play 빌드 단계가 `AFTERNOTE_STORE_DISTRIBUTED_BUILD=true`로 명시한 경우에만 true다.
 *   versionCode와 독립적이며 실제 설치 출처나 서명을 검사하는 값은 아니다.
 *   로컬·Firebase 빌드는 이 설정을 주입하지 않아 관문을 걸지 않는다(`docs/play-release.md`).
 */
data class InstalledBuild(
    val versionCode: Int,
    val storeDistributed: Boolean,
)
