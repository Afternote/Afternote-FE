package com.afternote.core.ui.button.FAB

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 화면 우측 하단에 떠 있는 대장 버튼(FAB).
 *
 * 자동으로 갖습니다. 크기나 패딩을 외부에서 받지 않습니다.
 */
@Composable
fun AfternoteFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        containerColor = AfternoteDesign.colors.gray9,
        contentColor = Color.White,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.core_ui_circle_button_plus),
            contentDescription = stringResource(R.string.core_ui_fab_content_description_add),
            modifier = Modifier.size(24.dp),
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
