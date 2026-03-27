package com.afternote.core.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray9

@Composable
fun FAB(modifier: Modifier = Modifier, onclick: () -> Unit) {
    SmallFloatingActionButton(
        onClick = {onclick()},
        containerColor = Gray9,
        shape = CircleShape,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(R.drawable.core_ui_add),
            contentDescription = null,
            tint = Color(0xFFFFFFFF),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview
@Composable
private fun FABPreview() {
    AfternoteTheme {
        FAB(
            onclick = {},
        )
    }
}