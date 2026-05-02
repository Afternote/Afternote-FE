package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

@Composable
fun TimeLetterTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    val shape = RoundedCornerShape(6.dp)
    val backgroundColor = if (isActive) AfternoteDesign.colors.black else AfternoteDesign.colors.gray2
    val textColor = if (isActive) AfternoteDesign.colors.white else AfternoteDesign.colors.gray6

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .width(61.dp)
                .height(36.dp)
                .clip(shape)
                .background(color = backgroundColor, shape = shape)
                .clickable(onClick = onClick)
                .padding(start = 18.dp, top = 8.dp, end = 18.dp, bottom = 8.dp),
    ) {
        Text(
            text = text,
            style = AfternoteDesign.typography.bodySmallR,
            color = textColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimeLetterTextButtonDefaultPrev() {
    TimeLetterTextButton(text = "수정", onClick = {})
}

@Preview(showBackground = true)
@Composable
private fun TimeLetterTextButtonActivePrev() {
    TimeLetterTextButton(text = "완료", onClick = {}, isActive = true)
}
