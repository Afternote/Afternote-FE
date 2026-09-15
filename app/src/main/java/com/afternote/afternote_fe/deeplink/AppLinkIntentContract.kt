package com.afternote.afternote_fe.deeplink

import android.content.Intent
import com.afternote.core.common.deeplink.AfternoteAppLinkParser
import com.afternote.core.common.deeplink.AppLinkResolution

/**
 * 검증된 App Link 가 Android intent 로 도착했다는 사실을 링크 해석 결과로 옮긴다 (#924).
 *
 * cold start 의 최초 intent 와 warm `onNewIntent` 가 **같은 함수**를 지나게 하려고 Activity 밖에
 * 둔다 — 두 경계가 각자 intent 를 읽으면 한쪽만 조건이 늘어나 목적지가 갈린다.
 *
 * 링크 문자열의 계약 판정은 전부 [AfternoteAppLinkParser] 가 갖는다. 여기서 하는 일은
 * 「이 intent 가 링크 진입인가」 하나뿐이다.
 */
internal object AppLinkIntentContract {
    /**
     * 링크 진입이면 해석 결과를, 아니면 `null` 을 돌려준다.
     *
     * [Intent.ACTION_VIEW] 가 아니면 링크가 아니다 — 런처 진입(`ACTION_MAIN`)과 알림 탭
     * PendingIntent 가 같은 Activity 로 오므로, 액션으로 먼저 갈라야 그 둘을 링크로 오인하지 않는다.
     *
     * VIEW 인데 data 가 없는 intent 는 `null` 이 아니라 **거절**이다. 매니페스트 필터가 data 를
     * 요구하므로 여기까지 왔다는 것은 우리가 모르는 경로로 들어왔다는 뜻이고, 그건 조용히
     * 무시할 일이 아니라 사유를 남길 일이다.
     */
    fun fromIntent(intent: Intent): AppLinkResolution? {
        val action = runCatching { intent.action }.getOrNull()
        if (action != Intent.ACTION_VIEW) return null

        val rawLink = runCatching { intent.dataString }.getOrNull()
        return AfternoteAppLinkParser.parse(rawLink)
    }
}
