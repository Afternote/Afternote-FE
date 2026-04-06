package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.Black
import com.afternote.core.ui.theme.Gray8
import com.afternote.core.ui.theme.Gray9
import com.afternote.core.ui.theme.Sansneo
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditReceiver

/**
 * 수신자 목록 카드. 애프터노트 상세 화면(갤러리/소셜/추모 가이드라인)에서 공통 사용.
 * 수신자가 없으면 아무것도 표시하지 않음.
 */
@Composable
fun ReceiversCard(
    modifier: Modifier = Modifier,
    receivers: List<AfternoteEditReceiver>,
) {
    if (receivers.isEmpty()) return

    InfoCard(
        modifier = modifier.fillMaxWidth(),
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                Text(
                    text = "수신자",
                    style =
                        TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontFamily = Sansneo,
                            fontWeight = FontWeight.Medium,
                            color = Gray9,
                        ),
                )
                receivers.forEach { receiver ->
                    ReceiverDetailItem(receiver = receiver)
                }
            }
        },
    )
}

@Composable
private fun ReceiverDetailItem(
    modifier: Modifier = Modifier,
    receiver: com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditReceiver,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(58.dp)
                    .clip(CircleShape),
        ) {
            Image(
                painter = painterResource(R.drawable.img_recipient_profile),
                contentDescription = "프로필 사진",
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column {
            Text(
                text = receiver.name,
                style =
                    TextStyle(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontFamily = Sansneo,
                        fontWeight = FontWeight.Medium,
                        color = Black,
                    ),
            )
            Text(
                text = receiver.label,
                style =
                    TextStyle(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontFamily = Sansneo,
                        fontWeight = FontWeight.Normal,
                        color = Gray8,
                    ),
            )
        }
    }
}
