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

@Composable
fun PenFloatingActionButton(
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
            painter = painterResource(id = R.drawable.core_ui_ic_plus_button_fab_pen),
            contentDescription = stringResource(R.string.core_ui_fab_content_description_add),
            modifier = Modifier.size(18.dp),
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
