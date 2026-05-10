package com.afternote.afternote_fe.screen.receiver.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.core.ui.topbar.HomeTopBar

/**
 * 수신자 홈 상단 바.
 *
 * 작성자 홈 [HomeTopBar]를 그대로 재사용한다 — 로고 + 프로필/설정 아이콘으로 동일하다.
 */
@Composable
fun ReceiverHomeTopBar(
    modifier: Modifier = Modifier,
    onSettingClick: () -> Unit = {},
) {
    HomeTopBar(modifier = modifier, onSettingClick = onSettingClick)
}
