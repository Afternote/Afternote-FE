package com.afternote.core.domain.repository.appversion

import com.afternote.core.model.appversion.AppVersionCheck

/**
 * 이 빌드가 아직 서버가 허용하는 버전인지 묻는 계약 (#1539).
 *
 * 인증이 필요 없다 — 서버가 이 엔드포인트를 화이트리스트에 두고 있어(BE `WhiteListUrl`)
 * 로그인 전에도 부를 수 있다.
 *
 * 실패는 예외로 던지지 않고 [Result] 로 돌려준다. 부르는 쪽(관문)이 «못 물어봤다» 를
 * «업데이트 불필요» 와 같게 다뤄야 하기 때문이다 — 서버가 죽어도 앱은 열려야 한다.
 */
interface AppVersionRepository {
    /** 이 Android 설치본의 [versionCode] 로 서버 판정을 받아 온다. */
    suspend fun checkAndroidVersion(versionCode: Int): Result<AppVersionCheck>
}
