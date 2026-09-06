package com.afternote.afternote_fe

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/**
 * 시스템바 아이콘까지 라이트로 고정한 edge-to-edge (#1719).
 *
 * 무인자 [enableEdgeToEdge] 는 `SystemBarStyle.auto` 라 `uiMode == NIGHT_YES` 면
 * `isAppearanceLightStatusBars` 를 `false` 로 잡아 **흰 아이콘**을 고른다. 그런데
 * `AfternoteTheme` 이 라이트로 잠긴 뒤로 Compose 는 다크 기기에서도 `gray1`(#FAFAFA) 을
 * 상태바 밑까지 그린다. 둘을 그대로 두면 밝은 배경 위에 흰 시계·배터리·제스처 바가 얹혀
 * 모든 화면에서 보이지 않는다.
 *
 * 그래서 시스템바 스타일도 함께 라이트로 못박는다. **`AfternoteTheme` 의 `isDarkTheme`
 * 기본값을 시스템 추종으로 되돌릴 때 이 함수도 같이 되돌린다** — 두 자리는 한 쌍이다.
 */
fun ComponentActivity.enableLightEdgeToEdge() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
    )
}
