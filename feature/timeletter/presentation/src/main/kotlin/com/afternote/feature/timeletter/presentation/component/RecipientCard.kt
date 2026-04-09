package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteDesign

@Composable
fun RecipientCard(
    recipientName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(AfternoteDesign.colors.gray2)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AfternoteDesign.colors.gray4),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = AfternoteDesign.colors.white,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "$recipientName 님에게",
            style = AfternoteDesign.typography.h3,
            color = AfternoteDesign.colors.gray9,
        )

        Spacer(modifier = Modifier.width(4.dp))

        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "수신인 변경",
            tint = AfternoteDesign.colors.gray6,
            modifier = Modifier.size(20.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(modifier = Modifier.width(56.dp)) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .background(AfternoteDesign.colors.gray3)
                        .border(2.dp, AfternoteDesign.colors.white, CircleShape),
            )
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .align(Alignment.CenterStart)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .border(2.dp, AfternoteDesign.colors.white, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "M",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
        }
    }
}
