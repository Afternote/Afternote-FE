package com.afternote.core.model.appversion

/**
 * 서버가 판정한 이 설치본의 강제 업데이트 필요 여부 (#1539).
 *
 * 서버 계약(`GET /api/v1/app/version`)을 그대로 옮긴 값이다. 판정 자체는 서버가 하고
 * (`versionCode < latestVersionCode`), 클라이언트는 그 결과를 어떻게 쓸지만 정한다.
 *
 * @property updateRequired 서버가 더 최신 릴리스를 알고 있어 이 빌드로는 계속 쓸 수 없다고 본 경우 true.
 * @property latestVersionCode 서버가 아는 최신 릴리스의 versionCode.
 * @property storeUrl 보낼 스토어 주소. 서버는 [updateRequired] 가 false 면 null 을 준다
 *   (BE `AppVersionService.checkVersion` 실코드 기준). true 인데도 비어 있으면 서버는 500 을 낸다.
 */
data class AppVersionCheck(
    val updateRequired: Boolean,
    val latestVersionCode: Int,
    val storeUrl: String?,
)
