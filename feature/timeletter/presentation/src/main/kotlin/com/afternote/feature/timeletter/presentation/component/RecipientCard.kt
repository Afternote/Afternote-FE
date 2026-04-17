package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.timeletter.presentation.R

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
                .height(115.dp)
                .background(AfternoteDesign.colors.gray2)
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.default_profile),
            contentDescription = "기본 프로필",
            modifier = Modifier.size(67.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        if (recipientName.isEmpty()) {
            Text(
                text = "수신자를 선택해주세요",
                style = AfternoteDesign.typography.h3,
                color = AfternoteDesign.colors.gray5,
            )
        } else {
            Text(
                text = "$recipientName 님에게",
                style = AfternoteDesign.typography.h3,
                color = AfternoteDesign.colors.gray9,
            )

            Spacer(modifier = Modifier.width(15.dp))

            Image(
                painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_right_arrow),
                contentDescription = "수신인 변경",
                modifier = Modifier.size(14.dp),
            )
        }

    }
}

@Preview
@Composable
private fun dkjfksjdprev() {
    RecipientCard(
        recipientName = "mini",
        onClick = {},
    )
}
