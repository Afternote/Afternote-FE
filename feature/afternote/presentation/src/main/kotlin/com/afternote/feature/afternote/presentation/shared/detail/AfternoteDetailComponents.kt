package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

@Composable
fun DetailSectionHeader(
    iconResId: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = AfternoteDesign.colors.gray5,
        )
        Text(
            text = label,
            style = AfternoteDesign.typography.mono,
            color = AfternoteDesign.colors.gray5,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = AfternoteDesign.colors.gray3,
        )
    }
}

@Composable
fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = AfternoteDesign.colors.gray2,
                    shape = RoundedCornerShape(8.dp),
                ).padding(16.dp),
    ) {
        content()
    }
}
