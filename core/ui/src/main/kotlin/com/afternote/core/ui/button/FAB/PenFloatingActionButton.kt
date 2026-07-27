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
 * 펜 글리프 FAB. [size] 기본값은 M3 기본(56dp)이라 기존 호출부는 무변경이다.
 * 시안(plus_button 48×48)에 맞추려면 호출부에서 [size] = 48.dp, [iconSize] = 17.dp 로 opt-in 한다.
 */
@Composable
fun PenFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 18.dp,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        containerColor = AfternoteDesign.colors.gray9,
        contentColor = AfternoteDesign.colors.white,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.core_ui_ic_plus_button_fab_pen),
            contentDescription = stringResource(R.string.core_ui_fab_content_description_add),
            modifier = Modifier.size(iconSize),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PenFloatingActionButtonPreview() {
    AfternoteTheme {
        PenFloatingActionButton(onClick = {})
    }
}
