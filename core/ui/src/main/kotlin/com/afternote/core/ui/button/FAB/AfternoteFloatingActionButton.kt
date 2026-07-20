package com.afternote.core.ui.button.FAB

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 화면 우측 하단에 떠 있는 대장 버튼(FAB).
 *
 * [size] 기본값은 M3 [FloatingActionButton] 기본(56dp)이라 기존 호출부는 무변경이다.
 * 시안(plus_button 48×48, 글리프 16.67dp)에 맞추려면 호출부에서 [size] = 48.dp, [iconSize] = 17.dp 로 opt-in 한다.
 * (벡터 viewport 14 / 글리프 13.6 을 Icon 이 ContentScale.Fit 로 확대 → 17 × 13.6/14 ≈ 16.5dp.)
 */
@Composable
fun AfternoteFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 24.dp,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        containerColor = AfternoteDesign.colors.gray9,
        contentColor = AfternoteDesign.colors.white,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.core_ui_circle_button_plus),
            contentDescription = stringResource(R.string.core_ui_fab_content_description_add),
            modifier = Modifier.size(iconSize),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteFloatingActionButtonPreview() {
    AfternoteTheme {
        AfternoteFloatingActionButton(onClick = {})
    }
}
