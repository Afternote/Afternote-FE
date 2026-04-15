package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 상세 화면용 정보 행: 원형 배경 아이콘 + 라벨·값 + (선택) 우측 슬롯.
 *
 * [DetailSection] / [DetailCard] 와는 별개의 **단일 행(Micro)** 컴포넌트로,
 * 카드 안·밖 어디서든 재사용할 수 있다.
 * 비밀번호 표시 토글 등 도메인 동작은 [trailingContent] 로만 주입한다.
 */
@Composable
fun DetailInfoRow(
    iconResId: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
//            .height(IntrinsicSize.Min)
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .clip(CircleShape)
                    .size(32.dp)
                    .background(AfternoteDesign.colors.gray2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                tint = AfternoteDesign.colors.gray9,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = AfternoteDesign.typography.footnoteCaption,
                color = AfternoteDesign.colors.gray6,
            )
            Text(
                text = value,
                style = AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.gray9,
            )
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.weight(1f))
            trailingContent()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailInfoRowPreview() {
    AfternoteTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DetailInfoRow(
                iconResId = com.afternote.core.ui.R.drawable.core_ui_ic_mail,
                label = "이메일",
                value = "afternote@example.com",
            )
            DetailInfoRow(
                iconResId = com.afternote.core.ui.R.drawable.core_ui_settings,
                label = "상태",
                value = "활성",
                trailingContent = {
                    AfternoteCircularCheckbox(
                        state = CheckboxState.Default,
                        size = 20.dp,
                    )
                },
            )
        }
    }
}
