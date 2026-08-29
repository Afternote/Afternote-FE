package com.afternote.core.ui.icon

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.afternote.core.ui.R

/**
 * 닫기(×) 벡터 아이콘. 크기는 호출부에서 [Modifier.size] 등으로 지정한다.
 *
 * @param modifier 레이아웃·크기 ([Modifier.size]), padding 등
 * @param contentDescription null이면 [R.string.core_ui_content_description_close] 사용
 * @param tint 기본값은 상위 [LocalContentColor]
 */
@Composable
fun CloseIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(R.drawable.core_ui_close),
        contentDescription = contentDescription ?: stringResource(R.string.core_ui_content_description_close),
        modifier = modifier,
        tint = tint,
    )
}
