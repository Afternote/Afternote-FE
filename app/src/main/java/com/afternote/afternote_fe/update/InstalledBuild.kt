package com.afternote.afternote_fe.update

/**
 * 지금 돌고 있는 설치본의 배포 정체성 (#1539).
 *
 * [ForceUpdateGate] 가 `BuildConfig` 를 직접 읽지 않고 이 값을 주입받는 이유는 단위 테스트다 —
 * `BuildConfig.VERSION_CODE` 는 빌드 시각에 고정돼 관문의 분기를 테스트로 덮을 수 없다.
 *
 * @property versionCode 서버에 보낼 이 설치본의 versionCode.
 * @property storeDistributed 이 빌드가 **스토어로 업데이트될 수 있는** 산출물인가.
 *   릴리스 워크플로가 `AFTERNOTE_VERSION_CODE` 를 주입한 빌드에서만 true 다
 *   (`build-logic` 의 `resolveAfternoteVersionCode` · `DEFAULT_AFTERNOTE_VERSION_CODE`).
 *   로컬·Firebase App Distribution 빌드는 기본값 versionCode 를 그대로 달고 있어 false 이고,
 *   서명 인증서도 Play 것과 달라 스토어가 그 위에 업데이트를 얹지 못한다 —
 *   보낼 수 없는 곳으로 보내는 대신 관문을 걸지 않는다(`docs/play-release.md`).
 */
data class InstalledBuild(
    val versionCode: Int,
    val storeDistributed: Boolean,
)
