package com.afternote.core.ui.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun AddCircleButton(
    contentDescription: String,
    onClick: () -> Unit,
    paddingValues: PaddingValues,
    plusSize: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(CircleShape)
            .background(AfternoteDesign.colors.black)
            .clickable(onClick = onClick)
            .padding(paddingValues),
    ) {
        Icon(
            painter = painterResource(R.drawable.core_ui_circle_button_plus),
            contentDescription = contentDescription,
            tint = AfternoteDesign.colors.white,
            modifier = Modifier.size(plusSize),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddCircleButtonPreview() {
    AfternoteTheme {
        AddCircleButton(
            contentDescription = "Add",
            onClick = {},
            paddingValues = PaddingValues(12.dp),
            plusSize = 24.dp,
        )
    }
}
